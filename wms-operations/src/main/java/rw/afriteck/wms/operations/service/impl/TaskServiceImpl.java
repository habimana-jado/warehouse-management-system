package rw.afriteck.wms.operations.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.foundation.support.Codes;
import rw.afriteck.wms.operations.dtos.*;
import rw.afriteck.wms.operations.entity.Task;
import rw.afriteck.wms.operations.entity.TaskEvent;
import rw.afriteck.wms.operations.entity.TaskLine;
import rw.afriteck.wms.operations.enums.*;
import rw.afriteck.wms.operations.mapper.TaskMapper;
import rw.afriteck.wms.operations.repository.TaskEventRepository;
import rw.afriteck.wms.operations.repository.TaskLineCount;
import rw.afriteck.wms.operations.repository.TaskRepository;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.operations.support.CurrentActor;
import rw.afriteck.wms.operations.support.TaskNumbers;
import rw.afriteck.wms.operations.support.TaskSpecs;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<TaskStatus> ASSIGNABLE = EnumSet.of(TaskStatus.OPEN, TaskStatus.ASSIGNED);
    private static final Set<TaskStatus> HELD = EnumSet.of(TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS);

    private final TaskRepository taskRepository;
    private final TaskEventRepository eventRepository;
    private final WarehouseService warehouseService;
    private final ItemService itemService;
    private final LocationService locationService;
    private final WorkerLookup workerLookup;
    private final CurrentActor actor;
    private final Clock clock;

    // ------------------------------------------------------------------ create

    @Override
    @Transactional
    public TaskResponse create(CreateTaskRequest r) {
        WarehouseResponse wh = warehouseService.getById(r.warehouseId());
        if (!wh.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is not active: " + wh.code());
        }
        if (r.lines() == null || r.lines().isEmpty()) {
            throw new BusinessException("TASK_NO_LINES", "A task needs at least one line");
        }

        Map<UUID, ItemResponse> items = indexItems(r.lines().stream()
                .map(CreateTaskLineRequest::itemId).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locations = indexLocations(r);

        if (r.zoneLocationId() != null) {
            LocationResponse zone = requireLocation(locations, r.zoneLocationId(), r.warehouseId());
            if (zone.type() != LocationType.ZONE) {
                throw new BusinessException("ZONE_INVALID", "zoneLocationId must be a ZONE location");
            }
        }

        Task t = new Task();
        t.setTaskNumber(TaskNumbers.format(taskRepository.nextTaskSequence()));
        t.setType(r.type());
        t.setStatus(TaskStatus.OPEN);
        t.setPriority(r.priority() == null ? TaskPriority.NORMAL : r.priority());
        t.setWarehouseId(r.warehouseId());
        t.setZoneLocationId(r.zoneLocationId());
        t.setReferenceType(Codes.normalize(r.referenceType()));
        t.setReferenceId(r.referenceId());
        t.setReferenceNumber(Codes.blankToNull(r.referenceNumber()));
        t.setDueAt(r.dueAt());
        t.setNotes(Codes.blankToNull(r.notes()));

        int lineNo = 1;
        for (CreateTaskLineRequest lr : r.lines()) {
            ItemResponse item = items.get(lr.itemId());
            if (item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item not found: " + lr.itemId());
            }
            if (!item.active()) {
                throw new BusinessException("ITEM_INACTIVE", "Item is not active: " + item.sku());
            }
            if (lr.plannedQty() == null || lr.plannedQty().signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Planned quantity must be positive: " + item.sku());
            }
            String lot = code(lr.lotNumber());
            String serial = code(lr.serialNumber());
            checkTracking(item, lot, serial, lr.plannedQty());

            requireBin(locations, lr.fromLocationId(), r.warehouseId());
            requireBin(locations, lr.toLocationId(), r.warehouseId());

            TaskLine l = new TaskLine();
            l.setLineNo(lineNo++);
            l.setItemId(lr.itemId());
            l.setLotNumber(lot);
            l.setSerialNumber(serial);
            l.setFromLocationId(lr.fromLocationId());
            l.setToLocationId(lr.toLocationId());
            l.setPlannedQty(lr.plannedQty());
            l.setDoneQty(BigDecimal.ZERO);
            l.setStatus(TaskLineStatus.PENDING);
            l.setReferenceLineId(lr.referenceLineId());
            t.addLine(l);
        }

        save(t);
        record(t, TaskEventType.CREATED, null, TaskStatus.OPEN, null, null);
        return toResponse(t);
    }

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findByReference(String referenceType, UUID referenceId) {
        return toResponses(taskRepository.findWithLinesByReference(Codes.normalize(referenceType), referenceId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskSummaryResponse> search(TaskFilter filter, Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable p = PageRequest.of(pageable.getPageNumber(), size,
                pageable.getSort().isSorted() ? pageable.getSort() : TaskSpecs.DEFAULT_SORT);

        Page<Task> page = taskRepository.findAll(TaskSpecs.from(filter), p);

        List<UUID> ids = page.getContent().stream().map(Task::getId).toList();
        Map<UUID, TaskLineCount> counts = ids.isEmpty() ? Map.of()
                : taskRepository.countLinesByTaskIds(ids).stream()
                .collect(Collectors.toMap(TaskLineCount::getTaskId, Function.identity()));
        Map<UUID, String> names = userNames(page.getContent().stream()
                .map(Task::getAssigneeId).filter(Objects::nonNull).collect(Collectors.toSet()));

        return PageResponse.of(page.map(t -> {
            TaskLineCount c = counts.get(t.getId());
            return TaskMapper.toSummary(t,
                    c == null ? 0 : (int) c.getTotal(),
                    c == null ? 0 : (int) c.getDone(),
                    names);
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskEventResponse> history(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new NotFoundException("Task", taskId);
        }
        List<TaskEvent> events = eventRepository.findByTaskIdOrderByOccurredAtAscCreatedAtAsc(taskId);
        Set<UUID> userIds = new HashSet<>();
        for (TaskEvent e : events) {
            if (e.getActorUserId() != null) userIds.add(e.getActorUserId());
            if (e.getAssigneeId() != null) userIds.add(e.getAssigneeId());
        }
        Map<UUID, String> names = userNames(userIds);
        return events.stream().map(e -> TaskMapper.toResponse(e, names)).toList();
    }

    // ------------------------------------------------------------------ assignment

    @Override
    @Transactional
    public TaskResponse assign(UUID taskId, UUID assigneeId) {
        Task t = load(taskId);
        if (!ASSIGNABLE.contains(t.getStatus())) {
            throw invalidState(t, "assigned");
        }
        if (assigneeId.equals(t.getAssigneeId())) {
            return toResponse(t);                               // nothing to change
        }
        requireAssignable(assigneeId, t.getWarehouseId());

        TaskStatus from = t.getStatus();
        boolean reassign = t.getAssigneeId() != null;
        applyAssignment(t, assigneeId);
        save(t);
        record(t, reassign ? TaskEventType.REASSIGNED : TaskEventType.ASSIGNED,
                from, TaskStatus.ASSIGNED, assigneeId, null);
        return toResponse(t);
    }

    @Override
    @Transactional
    public TaskResponse release(UUID taskId) {
        Task t = load(taskId);
        if (!HELD.contains(t.getStatus())) {
            throw invalidState(t, "released");
        }
        TaskStatus from = t.getStatus();
        UUID previous = t.getAssigneeId();
        t.setAssigneeId(null);
        t.setAssignedAt(null);
        t.setStartedAt(null);
        t.setStatus(TaskStatus.OPEN);
        save(t);
        record(t, TaskEventType.RELEASED, from, TaskStatus.OPEN, previous, null);
        return toResponse(t);
    }

    @Override
    @Transactional
    public TaskResponse start(UUID taskId) {
        Task t = load(taskId);
        if (t.getStatus() != TaskStatus.ASSIGNED) {
            throw invalidState(t, "started");
        }
        t.setStatus(TaskStatus.IN_PROGRESS);
        t.setStartedAt(now());
        save(t);
        record(t, TaskEventType.STARTED, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, t.getAssigneeId(), null);
        return toResponse(t);
    }

    @Override
    @Transactional
    public Optional<TaskResponse> claimNext(UUID warehouseId, Set<TaskType> types, UUID userId) {
        requireAssignable(userId, warehouseId);

        Collection<TaskType> wanted = (types == null || types.isEmpty())
                ? List.of(TaskType.values()) : types;
        List<String> typeNames = wanted.stream().map(Enum::name).toList();

        // FOR UPDATE SKIP LOCKED: concurrent pickers get different tasks, never the same one
        return taskRepository.lockNextOpen(warehouseId, typeNames).map(t -> {
            applyAssignment(t, userId);
            save(t);
            record(t, TaskEventType.ASSIGNED, TaskStatus.OPEN, TaskStatus.ASSIGNED, userId, "claimed");
            return toResponse(t);
        });
    }

    @Override
    @Transactional
    public TaskResponse changePriority(UUID taskId, TaskPriority priority) {
        Task t = load(taskId);
        if (!TaskStatus.ACTIVE.contains(t.getStatus())) {
            throw invalidState(t, "reprioritised");
        }
        if (t.getPriority() == priority) {
            return toResponse(t);
        }
        String note = t.getPriority() + " -> " + priority;
        t.setPriority(priority);
        save(t);
        record(t, TaskEventType.PRIORITY_CHANGED, t.getStatus(), t.getStatus(), t.getAssigneeId(), note);
        return toResponse(t);
    }

    // ------------------------------------------------------------------ completion / cancel

    @Override
    @Transactional
    public TaskResponse completeLine(UUID taskId, UUID lineId, BigDecimal doneQty, String shortReason) {
        Task t = load(taskId);
        if (!HELD.contains(t.getStatus())) {
            throw invalidState(t, "worked on (it must be ASSIGNED or IN_PROGRESS)");
        }
        TaskLine line = t.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("TaskLine", lineId));
        if (line.getStatus() != TaskLineStatus.PENDING) {
            throw new ConflictException("TASK_LINE_CLOSED",
                    "Line " + line.getLineNo() + " of " + t.getTaskNumber() + " is already " + line.getStatus());
        }
        if (doneQty == null || doneQty.signum() < 0) {
            throw new BusinessException("QTY_INVALID", "Done quantity cannot be negative");
        }
        if (doneQty.compareTo(line.getPlannedQty()) > 0) {
            throw new BusinessException("DONE_QTY_EXCEEDS_PLAN",
                    "Done quantity " + doneQty + " exceeds planned " + line.getPlannedQty());
        }

        Instant now = now();
        UUID assignee = t.getAssigneeId();

        // first closed line on a task nobody started: start it now so durations are never null
        if (t.getStatus() == TaskStatus.ASSIGNED) {
            t.setStatus(TaskStatus.IN_PROGRESS);
            t.setStartedAt(now);
            record(t, TaskEventType.STARTED, TaskStatus.ASSIGNED, TaskStatus.IN_PROGRESS, assignee,
                    "started with first line");
        }

        boolean isShort = doneQty.compareTo(line.getPlannedQty()) < 0;
        String reason = Codes.blankToNull(shortReason);
        if (isShort && reason == null) {
            throw new BusinessException("SHORT_REASON_REQUIRED",
                    "A reason is required when done quantity is below planned");
        }
        line.setDoneQty(doneQty);
        line.setStatus(isShort ? TaskLineStatus.SHORT : TaskLineStatus.DONE);
        line.setShortReason(isShort ? reason : null);
        record(t, TaskEventType.LINE_COMPLETED, t.getStatus(), t.getStatus(), assignee,
                "line " + line.getLineNo() + ": " + doneQty + "/" + line.getPlannedQty());

        boolean anyPending = t.getLines().stream().anyMatch(l -> l.getStatus() == TaskLineStatus.PENDING);
        if (!anyPending) {
            TaskStatus from = t.getStatus();
            t.setStatus(TaskStatus.COMPLETED);
            t.setCompletedAt(now);
            record(t, TaskEventType.COMPLETED, from, TaskStatus.COMPLETED, assignee, null);
        }
        save(t);
        return toResponse(t);
    }

    @Override
    @Transactional
    public TaskResponse cancel(UUID taskId, String reason) {
        Task t = load(taskId);
        doCancel(t, reason);
        return toResponse(t);
    }

    @Override
    @Transactional
    public int cancelByReference(String referenceType, UUID referenceId, String reason) {
        List<Task> active = taskRepository.findByReferenceTypeAndReferenceIdAndStatusIn(
                Codes.normalize(referenceType), referenceId, TaskStatus.ACTIVE);
        active.forEach(t -> doCancel(t, reason));
        return active.size();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAccessInfo accessInfo(UUID taskId) {
        return taskRepository.findAccessInfoById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));
    }

    private void doCancel(Task t, String reason) {
        if (!TaskStatus.ACTIVE.contains(t.getStatus())) {
            throw invalidState(t, "cancelled");
        }
        String r = Codes.blankToNull(reason);
        if (r == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancel reason is required");
        }
        TaskStatus from = t.getStatus();
        t.setStatus(TaskStatus.CANCELLED);
        t.setCancelReason(abbreviate(r, 255));
        save(t);
        record(t, TaskEventType.CANCELLED, from, TaskStatus.CANCELLED, t.getAssigneeId(), abbreviate(r, 255));
    }

    // ------------------------------------------------------------------ helpers: persistence

    private Task load(UUID id) {
        return taskRepository.findWithLinesById(id)
                .orElseThrow(() -> new NotFoundException("Task", id));
    }

    /** Flush so the @Version bump is visible in the response we are about to build. */
    private Task save(Task t) {
        return taskRepository.saveAndFlush(t);
    }

    private void record(Task t, TaskEventType type, TaskStatus from, TaskStatus to,
                        UUID assigneeId, String note) {
        TaskEvent e = new TaskEvent();
        e.setTask(t);
        e.setEventType(type);
        e.setFromStatus(from);
        e.setToStatus(to);
        e.setActorUserId(actor.idOrNull());          // null when done by a job
        e.setAssigneeId(assigneeId);
        e.setOccurredAt(now());
        e.setNote(note == null ? null : abbreviate(note, 255));
        eventRepository.save(e);
    }

    private void applyAssignment(Task t, UUID assigneeId) {
        t.setAssigneeId(assigneeId);
        t.setAssignedAt(now());
        t.setStatus(TaskStatus.ASSIGNED);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    // ------------------------------------------------------------------ helpers: validation

    private void requireAssignable(UUID userId, UUID warehouseId) {
        if (!workerLookup.isAssignableToWarehouse(userId, warehouseId)) {
            throw new BusinessException("ASSIGNEE_NOT_ALLOWED",
                    "User cannot be assigned to tasks in this warehouse");
        }
    }

    private ConflictException invalidState(Task t, String action) {
        return new ConflictException("TASK_INVALID_STATE",
                "Task " + t.getTaskNumber() + " is " + t.getStatus() + " and cannot be " + action);
    }

    private static void checkTracking(ItemResponse item, String lot, String serial, BigDecimal qty) {
        switch (item.trackingType()) {
            case NONE -> {
                if (lot != null || serial != null) {
                    throw new BusinessException("TRACKING_MISMATCH",
                            "Item " + item.sku() + " is not lot or serial tracked");
                }
            }
            case LOT -> {
                if (lot == null) {
                    throw new BusinessException("LOT_REQUIRED", "Lot number required for " + item.sku());
                }
                if (serial != null) {
                    throw new BusinessException("TRACKING_MISMATCH",
                            "Item " + item.sku() + " is lot tracked, not serial tracked");
                }
            }
            case SERIAL -> {
                if (serial == null) {
                    throw new BusinessException("SERIAL_REQUIRED", "Serial number required for " + item.sku());
                }
                if (lot != null) {
                    throw new BusinessException("TRACKING_MISMATCH",
                            "Item " + item.sku() + " is serial tracked, not lot tracked");
                }
                if (qty.compareTo(BigDecimal.ONE) != 0) {
                    throw new BusinessException("SERIAL_QTY_MUST_BE_ONE",
                            "Serial tracked line quantity must be 1 for " + item.sku());
                }
            }
        }
    }

    private static String code(String s) {
        String v = Codes.blankToNull(s);
        return v == null ? null : Codes.normalize(v);
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    // ------------------------------------------------------------------ helpers: lookups (batch)

    private Map<UUID, ItemResponse> indexItems(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return itemService.findAllByIds(ids).stream()
                .collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    /** One batch call for every location the request mentions (lines and zone). */
    private Map<UUID, LocationResponse> indexLocations(CreateTaskRequest r) {
        Set<UUID> ids = new HashSet<>();
        if (r.zoneLocationId() != null) ids.add(r.zoneLocationId());
        for (CreateTaskLineRequest l : r.lines()) {
            if (l.fromLocationId() != null) ids.add(l.fromLocationId());
            if (l.toLocationId() != null) ids.add(l.toLocationId());
        }
        if (ids.isEmpty()) return Map.of();
        return locationService.findAllByIds(ids).stream()
                .collect(Collectors.toMap(LocationResponse::id, Function.identity()));
    }

    private static LocationResponse requireLocation(Map<UUID, LocationResponse> locations,
                                                    UUID id, UUID warehouseId) {
        LocationResponse loc = locations.get(id);
        if (loc == null) {
            throw new BusinessException("LOCATION_NOT_FOUND", "Location not found: " + id);
        }
        if (!loc.warehouseId().equals(warehouseId)) {
            throw new BusinessException("LOCATION_WRONG_WAREHOUSE",
                    "Location " + loc.code() + " belongs to another warehouse");
        }
        return loc;
    }

    private static void requireBin(Map<UUID, LocationResponse> locations, UUID id, UUID warehouseId) {
        if (id == null) return;
        LocationResponse loc = requireLocation(locations, id, warehouseId);
        if (loc.type() != LocationType.BIN || !loc.active()) {
            throw new BusinessException("LOCATION_NOT_STORABLE",
                    "Location " + loc.code() + " must be an active BIN");
        }
    }

    private Map<UUID, String> userNames(Collection<UUID> ids) {
        return ids.isEmpty() ? Map.of() : workerLookup.findDisplayNames(ids);
    }

    private TaskResponse toResponse(Task t) {
        return toResponses(List.of(t)).get(0);
    }

    private List<TaskResponse> toResponses(Collection<Task> tasks) {
        Set<UUID> userIds = new HashSet<>();
        Set<UUID> itemIds = new HashSet<>();
        Set<UUID> locationIds = new HashSet<>();
        for (Task t : tasks) {
            if (t.getAssigneeId() != null) userIds.add(t.getAssigneeId());
            for (TaskLine l : t.getLines()) {
                itemIds.add(l.getItemId());
                if (l.getFromLocationId() != null) locationIds.add(l.getFromLocationId());
                if (l.getToLocationId() != null) locationIds.add(l.getToLocationId());
            }
        }
        Map<UUID, String> names = userNames(userIds);
        Map<UUID, ItemResponse> items = indexItems(itemIds);
        Map<UUID, LocationResponse> locations = locationIds.isEmpty() ? Map.of()
                : locationService.findAllByIds(locationIds).stream()
                .collect(Collectors.toMap(LocationResponse::id, Function.identity()));
        return tasks.stream().map(t -> TaskMapper.toResponse(t, names, items, locations)).toList();
    }
}

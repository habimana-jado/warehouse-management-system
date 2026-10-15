package rw.afriteck.wms.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.common.support.Chunks;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockBalance;
import rw.afriteck.wms.inventory.entity.StockCount;
import rw.afriteck.wms.inventory.entity.StockCountLine;
import rw.afriteck.wms.inventory.enums.CountLineStatus;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.mapper.CountMapper;
import rw.afriteck.wms.inventory.repository.LotRepository;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockCountLineRepository;
import rw.afriteck.wms.inventory.repository.StockCountRepository;
import rw.afriteck.wms.inventory.service.CountService;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.inventory.support.*;
import rw.afriteck.wms.operations.dtos.CreateTaskLineRequest;
import rw.afriteck.wms.operations.dtos.CreateTaskRequest;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskLineStatus;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountServiceImpl implements CountService {

    private static final int TASK_LINES = 50;
    private static final Set<String> SORTS = Set.of("createdAt", "countNumber", "status");
    private static final Set<String> LINE_SORTS = Set.of("lineNo", "countedAt");
    private static final Set<CountStatus> ACTIVE = EnumSet.of(CountStatus.IN_PROGRESS, CountStatus.REVIEW);

    private final StockCountRepository counts;
    private final StockCountLineRepository countLines;
    private final StockBalanceRepository balances;
    private final LotRepository lots;
    private final InventoryService inventory;
    private final TaskService taskService;
    private final WarehouseService warehouseService;
    private final ItemService itemService;
    private final LocationService locationService;
    private final WorkerLookup workers;
    private final Clock clock;

    // ================================================================== create / start

    @Override
    @Transactional
    public CountResponse create(CreateCountRequest r) {
        if (r == null || r.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        WarehouseResponse warehouse = warehouseService.getById(r.warehouseId());
        if (!warehouse.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        if (r.zoneLocationId() != null) {
            LocationResponse zone = locationService.getById(r.zoneLocationId());
            if (zone.type() != LocationType.ZONE || !zone.warehouseId().equals(warehouse.id())) {
                throw new BusinessException("ZONE_INVALID", "The scope must be a ZONE of the warehouse");
            }
        }
        StockCount c = new StockCount();
        c.setWarehouseId(warehouse.id());
        c.setZoneLocationId(r.zoneLocationId());
        c.setNotes(Codes.blankToNull(r.notes()));
        c.setStatus(CountStatus.DRAFT);
        c.setCountNumber(InventoryNumbers.count(counts.nextNumber()));
        return toResponse(counts.saveAndFlush(c));
    }

    @Override
    @Transactional
    public CountResponse start(UUID id, StartCountRequest r) {
        StockCount c = load(id);
        requireStatus(c, CountStatus.DRAFT);
        if (!warehouseService.getById(c.getWarehouseId()).active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        assertNoOverlap(c);

        List<StockBalance> rows = new ArrayList<>();
        if (c.getZoneLocationId() == null) {
            rows.addAll(balances.findAllPositive(c.getWarehouseId()));
        } else {
            List<UUID> binIds = locationService.findBinsUnder(c.getZoneLocationId()).stream()
                    .map(LocationResponse::id).toList();
            for (List<UUID> chunk : Chunks.partition(binIds)) {
                rows.addAll(balances.findPositiveInLocations(chunk));
            }
        }
        if (rows.isEmpty()) {
            throw new BusinessException("COUNT_NOTHING_TO_COUNT", "There is no stock in the scope of this count");
        }

        Map<UUID, LocationResponse> locations = locationsById(
                rows.stream().map(StockBalance::getLocationId).collect(Collectors.toSet()));
        rows.sort(Comparator
                .comparing((StockBalance b) -> locations.containsKey(b.getLocationId()) ? locations.get(b.getLocationId()).code() : "")
                .thenComparing(StockBalance::getItemId));

        List<StockCountLine> lines = new ArrayList<>(rows.size());
        int lineNo = 1;
        for (StockBalance b : rows) {
            StockCountLine l = new StockCountLine();
            l.setCount(c);
            l.setLineNo(lineNo++);
            l.setLocationId(b.getLocationId());
            l.setItemId(b.getItemId());
            l.setLot(b.getLot());
            l.setSerialNumber(b.getSerialNumber());
            l.setExpectedQty(b.getQuantity());
            l.setStatus(CountLineStatus.PENDING);
            lines.add(l);
        }
        countLines.saveAll(lines);                                  // ids exist from here on

        createTasks(c, lines, r == null ? null : r.priority(), r == null ? null : r.dueAt());
        c.setStatus(CountStatus.IN_PROGRESS);
        c.setStartedAt(clock.instant());
        return toResponse(counts.saveAndFlush(c));
    }

    // ================================================================== counting

    @Override
    @Transactional
    public CountLineResponse recordCount(UUID id, UUID lineId, RecordCountRequest r, UUID actorId, boolean privileged) {
        requireActor(actorId);
        StockCount c = load(id);
        requireStatus(c, CountStatus.IN_PROGRESS);
        StockCountLine line = countLines.findByIdAndCountId(lineId, id)
                .orElseThrow(() -> new NotFoundException("StockCountLine", lineId));
        BigDecimal counted = validateCounted(line, r == null ? null : r.countedQty());

        if (line.getStatus() == CountLineStatus.PENDING) {          // first count completes the task line; recounts do not
            completeTaskLine(line, actorId, privileged);
        }
        line.setExpectedQty(currentQty(line));                      // the system quantity AT counting time (see design point 1)
        line.setCountedQty(counted);
        line.setStatus(CountLineStatus.COUNTED);
        line.setCountedBy(actorId);
        line.setCountedAt(clock.instant());
        return toLine(countLines.saveAndFlush(line));
    }

    @Override
    @Transactional
    public CountLineResponse addLine(UUID id, AddCountLineRequest r, UUID actorId) {
        requireActor(actorId);
        StockCount c = load(id);
        requireStatus(c, CountStatus.IN_PROGRESS);
        if (r == null || r.itemId() == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "Item is required");
        }
        if (r.countedQty() == null || r.countedQty().signum() <= 0
                || r.countedQty().stripTrailingZeros().scale() > StockLineRules.SCALE) {
            throw new BusinessException("QTY_INVALID", "Found quantity must be above zero with at most 4 decimals");
        }
        ItemResponse item = itemService.getById(r.itemId());
        LocationResponse loc = locationService.requireStorableBin(r.locationId());
        if (!loc.warehouseId().equals(c.getWarehouseId())) {
            throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Bin " + loc.code() + " is in another warehouse");
        }
        if (c.getZoneLocationId() != null && locationService.findBinsUnder(c.getZoneLocationId()).stream()
                .noneMatch(b -> b.id().equals(loc.id()))) {
            throw new BusinessException("COUNT_LOCATION_OUT_OF_SCOPE", "Bin " + loc.code() + " is outside the counted zone");
        }
        String lotNumber = InventoryCodes.normalizeOrNull(r.lotNumber());
        String serial = InventoryCodes.normalizeOrNull(r.serialNumber());
        StockLineRules.check(MovementType.COUNT_ADJUSTMENT, item, r.countedQty(), lotNumber, null, serial);
        Lot lot = lotNumber == null ? null : lots.findByItemIdAndLotNumber(item.id(), lotNumber)
                .orElseThrow(() -> new BusinessException("LOT_NOT_FOUND",
                        "Lot " + lotNumber + " does not exist: use an adjustment (FOUND) for unknown lots"));

        StockCountLine line = new StockCountLine();
        line.setCount(c);
        line.setLineNo(countLines.maxLineNo(id) + 1);
        line.setLocationId(loc.id());
        line.setItemId(item.id());
        line.setLot(lot);
        line.setSerialNumber(serial);
        line.setExpectedQty(currentQty(line));                      // usually zero: that is the finding
        line.setCountedQty(r.countedQty());
        line.setStatus(CountLineStatus.COUNTED);
        line.setCountedBy(actorId);
        line.setCountedAt(clock.instant());
        return toLine(countLines.saveAndFlush(line));               // the unique index rejects a duplicate key (409)
    }

    // ================================================================== review / approval / cancel

    @Override
    @Transactional
    public CountResponse submit(UUID id) {
        StockCount c = load(id);
        requireStatus(c, CountStatus.IN_PROGRESS);
        long pending = countLines.countByCountIdAndStatus(id, CountLineStatus.PENDING);
        if (pending > 0) {
            throw new BusinessException("COUNT_LINES_PENDING", pending + " lines are still to be counted");
        }
        taskService.cancelByReference(InventoryReferences.STOCK_COUNT, id, "Count submitted for review");   // stragglers only
        c.setStatus(CountStatus.REVIEW);
        return toResponse(counts.saveAndFlush(c));
    }

    @Override
    @Transactional
    public CountResponse reopen(UUID id) {
        StockCount c = load(id);
        requireStatus(c, CountStatus.REVIEW);
        c.setStatus(CountStatus.IN_PROGRESS);
        return toResponse(counts.saveAndFlush(c));
    }

    @Override
    @Transactional
    public CountResponse approve(UUID id, UUID approverId) {
        requireActor(approverId);
        StockCount c = load(id);
        requireStatus(c, CountStatus.REVIEW);
        List<StockCountLine> variance = countLines.findAllVariance(id);
        if (!variance.isEmpty()) {
            List<PostStockLine> posting = variance.stream()
                    .map(l -> new PostStockLine(MovementType.COUNT_ADJUSTMENT, l.getLocationId(), l.getItemId(),
                            l.getLot() == null ? null : l.getLot().getLotNumber(), null, l.getSerialNumber(),
                            l.getCountedQty().subtract(l.getExpectedQty()), null))
                    .toList();
            inventory.post(new PostStockRequest(c.getId(), null, InventoryReferences.STOCK_COUNT, c.getId(),
                    c.getCountNumber(), "Cycle count variance", posting));
        }
        c.setStatus(CountStatus.COMPLETED);
        c.setApprovedBy(approverId);
        c.setCompletedAt(clock.instant());
        return toResponse(counts.saveAndFlush(c));
    }

    @Override
    @Transactional
    public CountResponse cancel(UUID id, String reason) {
        String text = Codes.blankToNull(reason);
        if (text == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancellation reason is required");
        }
        StockCount c = load(id);
        if (c.getStatus() == CountStatus.COMPLETED || c.getStatus() == CountStatus.CANCELLED) {
            throw new ConflictException("COUNT_INVALID_STATE", "Count " + c.getCountNumber() + " is " + c.getStatus());
        }
        if (c.getStatus() != CountStatus.DRAFT) {
            taskService.cancelByReference(InventoryReferences.STOCK_COUNT, id, text);
        }
        c.setStatus(CountStatus.CANCELLED);                         // nothing was posted before approval
        c.setCancelReason(text);
        c.setCompletedAt(clock.instant());
        return toResponse(counts.saveAndFlush(c));
    }

    // ================================================================== reads

    @Override
    @Transactional(readOnly = true)
    public CountResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID warehouseIdOf(UUID id) {
        return counts.findById(id).map(StockCount::getWarehouseId)
                .orElseThrow(() -> new NotFoundException("StockCount", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CountResponse> search(CountFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        Set<CountStatus> statuses = (f.statuses() == null || f.statuses().isEmpty())
                ? EnumSet.allOf(CountStatus.class) : f.statuses();
        Pageable p = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockCount> page = counts.search(f.warehouseId(), statuses, Codes.likePattern(f.q()), p);

        Map<UUID, StockCountLineRepository.LineStats> stats = new HashMap<>();
        List<UUID> ids = page.getContent().stream().map(StockCount::getId).toList();
        if (!ids.isEmpty()) {
            countLines.stats(ids).forEach(s -> stats.put(s.getCountId(), s));
        }
        Set<UUID> approvers = page.getContent().stream().map(StockCount::getApprovedBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = approvers.isEmpty() ? Map.of() : workers.findDisplayNames(approvers);
        return PageResponse.of(page.map(c -> {
            StockCountLineRepository.LineStats s = stats.get(c.getId());
            return CountMapper.toResponse(c, s == null ? 0 : s.getTotal(), s == null ? 0 : s.getCounted(), null, names);
        }));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CountLineResponse> lines(UUID id, boolean onlyVariance, Pageable pageable) {
        counts.findById(id).orElseThrow(() -> new NotFoundException("StockCount", id));
        Pageable p = PageRules.sanitize(pageable, LINE_SORTS, Sort.by("lineNo"));
        Page<StockCountLine> page = onlyVariance ? countLines.findVariance(id, p) : countLines.findByCountId(id, p);

        Map<UUID, ItemResponse> items = itemsById(page.getContent().stream().map(StockCountLine::getItemId).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locs = locationsById(page.getContent().stream().map(StockCountLine::getLocationId).collect(Collectors.toSet()));
        Set<UUID> counters = page.getContent().stream().map(StockCountLine::getCountedBy)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = counters.isEmpty() ? Map.of() : workers.findDisplayNames(counters);
        return PageResponse.of(page.map(l ->
                CountMapper.toLine(l, items.get(l.getItemId()), locs.get(l.getLocationId()), names)));
    }

    // ================================================================== helpers

    private void createTasks(StockCount c, List<StockCountLine> lines, TaskPriority priority, Instant dueAt) {
        Map<UUID, StockCountLine> byId = lines.stream().collect(Collectors.toMap(StockCountLine::getId, Function.identity()));
        for (int from = 0; from < lines.size(); from += TASK_LINES) {
            List<StockCountLine> part = lines.subList(from, Math.min(from + TASK_LINES, lines.size()));
            List<CreateTaskLineRequest> taskLines = part.stream()
                    .map(l -> new CreateTaskLineRequest(l.getItemId(),
                            l.getLot() == null ? null : l.getLot().getLotNumber(), l.getSerialNumber(),
                            l.getLocationId(), null, l.getExpectedQty(), l.getId()))
                    .toList();
            TaskResponse task = taskService.create(new CreateTaskRequest(TaskType.COUNT, priority,
                    c.getWarehouseId(), c.getZoneLocationId(), InventoryReferences.STOCK_COUNT, c.getId(),
                    c.getCountNumber(), dueAt, c.getNotes(), taskLines));
            for (TaskLineResponse tl : task.lines()) {              // remember which task line carries each count line
                StockCountLine l = byId.get(tl.referenceLineId());
                if (l != null) {
                    l.setTaskId(task.id());
                    l.setTaskLineId(tl.id());
                }
            }
        }
    }

    /** Closes the task line as "counted". A manager may count even when the task is not active. */
    private void completeTaskLine(StockCountLine line, UUID actorId, boolean privileged) {
        if (line.getTaskId() == null || line.getTaskLineId() == null) {
            return;
        }
        TaskResponse task = taskService.getById(line.getTaskId());
        boolean active = task.status() == TaskStatus.ASSIGNED || task.status() == TaskStatus.IN_PROGRESS;
        if (!active) {
            if (privileged) {
                return;
            }
            throw new ConflictException("COUNT_TASK_NOT_STARTED", "Claim or assign the count task before counting");
        }
        if (!privileged && !actorId.equals(task.assigneeId())) {
            throw new ForbiddenException("This task is assigned to another user");
        }
        task.lines().stream()
                .filter(tl -> tl.id().equals(line.getTaskLineId()) && tl.status() == TaskLineStatus.PENDING)
                .findFirst()
                .ifPresent(tl -> taskService.completeLine(task.id(), tl.id(), tl.plannedQty(), null));
    }

    /** Live quantity of the line's exact key, without locking. */
    private BigDecimal currentQty(StockCountLine l) {
        Optional<BigDecimal> q;
        if (l.getSerialNumber() != null) {
            q = balances.quantitySerial(l.getLocationId(), l.getItemId(), l.getSerialNumber());
        } else if (l.getLot() != null) {
            q = balances.quantityLot(l.getLocationId(), l.getItemId(), l.getLot().getId());
        } else {
            q = balances.quantityUntracked(l.getLocationId(), l.getItemId());
        }
        return q.orElse(BigDecimal.ZERO);
    }

    private static BigDecimal validateCounted(StockCountLine l, BigDecimal q) {
        if (q == null || q.signum() < 0 || q.stripTrailingZeros().scale() > StockLineRules.SCALE
                || (l.getSerialNumber() != null && q.compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessException("QTY_INVALID",
                    "Counted quantity must be zero or more with at most 4 decimals (0 or 1 for a serial)");
        }
        return q;
    }

    private void assertNoOverlap(StockCount c) {
        for (StockCount other : counts.findByWarehouseIdAndStatusIn(c.getWarehouseId(), ACTIVE)) {
            boolean overlap = other.getZoneLocationId() == null || c.getZoneLocationId() == null
                    || other.getZoneLocationId().equals(c.getZoneLocationId());
            if (overlap) {
                throw new ConflictException("COUNT_ALREADY_ACTIVE",
                        "Count " + other.getCountNumber() + " already covers this area");
            }
        }
    }

    private static void requireStatus(StockCount c, CountStatus expected) {
        if (c.getStatus() != expected) {
            throw new ConflictException("COUNT_INVALID_STATE",
                    "Count " + c.getCountNumber() + " is " + c.getStatus() + ", expected " + expected);
        }
    }

    private static void requireActor(UUID actorId) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "A user is required for this operation");
        }
    }

    private StockCount load(UUID id) {
        return counts.findById(id).orElseThrow(() -> new NotFoundException("StockCount", id));
    }

    private CountResponse toResponse(StockCount c) {
        long total = 0;
        long counted = 0;
        List<StockCountLineRepository.LineStats> stats = countLines.stats(List.of(c.getId()));
        if (!stats.isEmpty()) {
            total = stats.get(0).getTotal();
            counted = stats.get(0).getCounted();
        }
        long variance = countLines.countVariance(c.getId());
        Map<UUID, String> names = c.getApprovedBy() == null ? Map.of() : workers.findDisplayNames(List.of(c.getApprovedBy()));
        return CountMapper.toResponse(c, total, counted, variance, names);
    }

    private CountLineResponse toLine(StockCountLine l) {
        Map<UUID, String> names = l.getCountedBy() == null ? Map.of() : workers.findDisplayNames(List.of(l.getCountedBy()));
        return CountMapper.toLine(l,
                itemsById(Set.of(l.getItemId())).get(l.getItemId()),
                locationsById(Set.of(l.getLocationId())).get(l.getLocationId()), names);
    }

    private Map<UUID, ItemResponse> itemsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return itemService.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    private Map<UUID, LocationResponse> locationsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return locationService.findAllByIds(ids).stream().collect(Collectors.toMap(LocationResponse::id, Function.identity()));
    }
}

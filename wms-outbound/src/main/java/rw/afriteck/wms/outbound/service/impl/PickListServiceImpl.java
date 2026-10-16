package rw.afriteck.wms.outbound.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.support.Chunks;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.PostStockLine;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.operations.dtos.CreateTaskLineRequest;
import rw.afriteck.wms.operations.dtos.CreateTaskRequest;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.*;
import rw.afriteck.wms.outbound.enums.PickLineStatus;
import rw.afriteck.wms.outbound.enums.PickListStatus;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.mappers.PickListMapper;
import rw.afriteck.wms.outbound.repository.PickLineRepository;
import rw.afriteck.wms.outbound.repository.PickListRepository;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.support.OutboundNumbers;
import rw.afriteck.wms.outbound.support.OutboundReferences;
import rw.afriteck.wms.outbound.support.PickSuggester;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PickListServiceImpl implements PickListService {

    private static final Set<String> SORTS = Set.of("releasedAt", "pickNumber", "status", "createdAt");

    private final SalesOrderRepository orders;
    private final PickListRepository lists;
    private final PickLineRepository pickLines;
    private final TaskService taskService;
    private final InventoryService inventory;
    private final InventoryQueryService inventoryQuery;
    private final ItemService items;
    private final LocationService locations;
    private final CustomerService customers;
    private final WarehouseService warehouses;
    private final PickSuggester suggester;
    private final OutboundNumbers numbers;
    private final Clock clock;

    private record LotKey(UUID locationId, String lotNumber) {
    }

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public PickListResponse getById(UUID id) {
        return toResponses(List.of(load(id))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public PickListResponse getByTaskId(UUID taskId) {
        PickList p = lists.findByTaskId(taskId).orElseThrow(() -> new NotFoundException("PickList", taskId));
        return toResponses(List.of(p)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickListResponse> findBySalesOrder(UUID salesOrderId) {
        return toResponses(lists.findBySalesOrderIdOrderByReleasedAtDesc(salesOrderId));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID warehouseIdOf(UUID pickListId) {
        return load(pickListId).getWarehouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PickListSummaryResponse> search(UUID warehouseId, Set<PickListStatus> statuses, Pageable pageable) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
        Set<PickListStatus> wanted = (statuses == null || statuses.isEmpty())
                ? EnumSet.allOf(PickListStatus.class) : statuses;          // never an empty IN list
        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "releasedAt"));
        Page<PickList> page = lists.search(warehouseId, wanted, safe);

        List<UUID> ids = page.getContent().stream().map(PickList::getId).toList();
        Map<UUID, Long> total = new HashMap<>();
        Map<UUID, Long> pending = new HashMap<>();
        if (!ids.isEmpty()) {
            for (List<UUID> chunk : Chunks.partition(ids)) {
                lists.countLines(chunk).forEach(c -> total.put(c.getParentId(), c.getTotal()));
                lists.countLinesByStatus(chunk, PickLineStatus.PENDING).forEach(c -> pending.put(c.getParentId(), c.getTotal()));
            }
        }
        return PageResponse.of(page.map(p -> {
            int all = total.getOrDefault(p.getId(), 0L).intValue();
            int open = pending.getOrDefault(p.getId(), 0L).intValue();
            return PickListMapper.toSummary(p, all, all - open);
        }));
    }

    // ---------------------------------------------------------------- release

    @Override
    public PickListResponse release(UUID salesOrderId, ReleaseSalesOrderRequest r) {
        SalesOrder o = orders.findByIdForUpdate(salesOrderId)
                .orElseThrow(() -> new NotFoundException("SalesOrder", salesOrderId));
        if (!SalesOrderStatus.RELEASABLE.contains(o.getStatus())) {
            throw new ConflictException("SO_INVALID_STATE", "The order is " + o.getStatus() + ": it cannot be released");
        }
        if (!warehouses.getById(o.getWarehouseId()).active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "The warehouse is inactive");
        }
        List<SalesOrderLine> open = o.getLines().stream().filter(l -> l.unreleasedQty().signum() > 0).toList();
        if (open.isEmpty()) {
            throw new BusinessException("SO_NOTHING_TO_RELEASE", "Everything on this order is already released");
        }
        String notes = r == null ? null : Codes.blankToNull(r.notes());
        checkLength(notes, 1000, "Notes");

        List<PickSuggester.Suggestion> suggestions = suggester.suggest(o.getWarehouseId(),
                open.stream().map(l -> new PickSuggester.Need(l.getItemId(), l.unreleasedQty())).toList());

        PickList p = new PickList();
        p.setPickNumber(numbers.nextPickList());
        p.setSalesOrder(o);
        p.setWarehouseId(o.getWarehouseId());
        p.setStatus(PickListStatus.RELEASED);
        p.setReleasedAt(clock.instant());
        for (int i = 0; i < open.size(); i++) {
            SalesOrderLine sol = open.get(i);
            PickSuggester.Suggestion s = suggestions.get(i);
            PickLine pl = new PickLine();
            pl.setLineNo(i + 1);
            pl.setSalesOrderLine(sol);
            pl.setItemId(sol.getItemId());
            pl.setPlannedQty(sol.unreleasedQty());
            pl.setSuggestedLocationId(s.locationId());
            pl.setSuggestedLotNumber(s.lotNumber());
            p.addLine(pl);
            sol.setReleasedQty(sol.getReleasedQty().add(pl.getPlannedQty()));
        }
        p = lists.saveAndFlush(p);                                    // ids are needed as task references

        Instant dueAt = r != null && r.dueAt() != null ? r.dueAt()
                : o.getRequiredDate() == null ? null
                : o.getRequiredDate().plusDays(1).atStartOfDay(clock.getZone()).toInstant();   // end of the required day
        TaskResponse task = taskService.create(new CreateTaskRequest(
                TaskType.PICK, r == null ? null : r.priority(), p.getWarehouseId(), null,
                OutboundReferences.PICK_LIST, p.getId(), p.getPickNumber(), dueAt, notes,
                p.getLines().stream().map(pl -> new CreateTaskLineRequest(
                        pl.getItemId(), pl.getSuggestedLotNumber(), null,
                        pl.getSuggestedLocationId(), null, pl.getPlannedQty(), pl.getId())).toList()));

        Map<UUID, UUID> taskLineByPickLine = new HashMap<>();
        task.lines().forEach(tl -> taskLineByPickLine.put(tl.referenceLineId(), tl.id()));
        p.setTaskId(task.id());
        p.getLines().forEach(pl -> pl.setTaskLineId(taskLineByPickLine.get(pl.getId())));
        p = lists.saveAndFlush(p);
        return toResponses(List.of(p)).get(0);
    }

    // ------------------------------------------------------- pick confirmation

    @Override
    public PickListResponse confirmPick(UUID pickListId, UUID lineId, ConfirmPickRequest r, UUID actorId, boolean privileged) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "The acting user is required");
        }
        List<PickAllocationRequest> requested = r == null || r.allocations() == null ? List.of() : r.allocations();
        String shortReason = r == null ? null : Codes.blankToNull(r.shortReason());
        boolean expiryOverride = r != null && r.expiryOverride();
        checkLength(shortReason, 500, "Short reason");

        // lock order: the order header first, inventory balances later (inside inventory.post)
        UUID orderId = pickLines.findSalesOrderIdById(lineId).orElseThrow(() -> new NotFoundException("PickLine", lineId));
        orders.findByIdForUpdate(orderId).orElseThrow(() -> new NotFoundException("SalesOrder", orderId));
        PickLine line = pickLines.findById(lineId).orElseThrow(() -> new NotFoundException("PickLine", lineId));
        PickList list = line.getPickList();
        if (!list.getId().equals(pickListId)) {
            throw new NotFoundException("PickLine", lineId);
        }
        if (!PickListStatus.OPEN.contains(list.getStatus())) {
            throw new ConflictException("PICK_LIST_INVALID_STATE", "The pick list is " + list.getStatus());
        }
        if (line.getStatus() != PickLineStatus.PENDING) {
            throw new ConflictException("PICK_LINE_CLOSED", "This pick line is already closed");
        }
        TaskResponse task = taskService.getById(list.getTaskId());
        if (task.status() != TaskStatus.ASSIGNED && task.status() != TaskStatus.IN_PROGRESS) {
            throw new ConflictException("PICK_TASK_NOT_STARTED", "The pick task must be claimed or assigned first");
        }
        if (!privileged && !actorId.equals(task.assigneeId())) {
            throw new ForbiddenException("This pick belongs to another user");
        }

        ItemResponse item = items.getById(line.getItemId());
        List<PickAllocation> allocations = buildAllocations(list, item, requested, expiryOverride, privileged);

        BigDecimal planned = line.getPlannedQty();
        BigDecimal total = allocations.stream().map(PickAllocation::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(planned) > 0) {
            throw new BusinessException("DONE_QTY_EXCEEDS_PLAN", "Picked quantity is above the planned quantity");
        }
        boolean isShort = total.compareTo(planned) < 0;
        if (isShort && shortReason == null) {
            throw new BusinessException("SHORT_REASON_REQUIRED", "A reason is required when picking less than planned");
        }

        // 1. stock first (negative deltas), 2. our rows, 3. the task line
        if (!allocations.isEmpty()) {
            inventory.post(new PostStockRequest(null, null, OutboundReferences.PICK_LIST, list.getId(), list.getPickNumber(),
                    null, allocations.stream().map(a -> new PostStockLine(MovementType.ISSUE, a.getLocationId(), line.getItemId(),
                    a.getLotNumber(), null, a.getSerialNumber(), a.getQuantity().negate(), null)).toList()));
        }
        allocations.forEach(line::addAllocation);
        line.setPickedQty(total);
        line.setStatus(isShort ? PickLineStatus.SHORT : PickLineStatus.PICKED);
        line.setShortReason(isShort ? shortReason : null);

        SalesOrderLine sol = line.getSalesOrderLine();
        sol.setPickedQty(sol.getPickedQty().add(total));
        sol.setReleasedQty(sol.getReleasedQty().subtract(planned.subtract(total)));   // the shortage becomes a backorder

        taskService.completeLine(list.getTaskId(), line.getTaskLineId(), total, isShort ? shortReason : null);

        boolean anyPending = list.getLines().stream().anyMatch(l -> l.getStatus() == PickLineStatus.PENDING);
        if (anyPending) {
            list.setStatus(PickListStatus.PICKING);
        } else {
            list.setStatus(PickListStatus.PICKED);
            list.setCompletedAt(clock.instant());
        }
        return toResponses(List.of(lists.saveAndFlush(list))).get(0);
    }

    /**
     * Validates every allocation (tracking, bin, expiry) and returns detached rows; nothing is posted here.
     */
    private List<PickAllocation> buildAllocations(PickList list, ItemResponse item, List<PickAllocationRequest> requested,
                                                  boolean expiryOverride, boolean privileged) {
        List<PickAllocation> out = new ArrayList<>();
        if (requested.isEmpty()) return out;

        Set<UUID> locationIds = new HashSet<>();
        for (PickAllocationRequest a : requested) {
            if (a == null || a.locationId() == null) {
                throw new BusinessException("LOCATION_NOT_FOUND", "Every allocation needs a location");
            }
            locationIds.add(a.locationId());
        }
        Map<UUID, LocationResponse> bins = new HashMap<>();
        locations.findAllByIds(locationIds).forEach(l -> bins.put(l.id(), l));
        Map<LotKey, LocalDate> expiries = item.expiryTracked() ? loadExpiries(list.getWarehouseId(), item.id()) : Map.of();
        LocalDate today = LocalDate.now(clock);
        Set<String> serials = new HashSet<>();

        for (PickAllocationRequest a : requested) {
            LocationResponse bin = bins.get(a.locationId());
            if (bin == null) throw new BusinessException("LOCATION_NOT_FOUND", "Location not found: " + a.locationId());
            if (!bin.warehouseId().equals(list.getWarehouseId())) {
                throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Location " + bin.code() + " is in another warehouse");
            }
            if (bin.usage() != LocationUsage.STORAGE) {
                throw new BusinessException("LOCATION_USAGE_INVALID", "Goods are picked from STORAGE bins only: " + bin.code());
            }
            if (a.quantity() == null || a.quantity().signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Picked quantity must be greater than 0");
            }
            String lot = normalizeOrNull(a.lotNumber());
            String serial = normalizeOrNull(a.serialNumber());
            switch (item.trackingType()) {
                case NONE -> {
                    if (lot != null || serial != null) {
                        throw new BusinessException("TRACKING_MISMATCH", item.sku() + " has no lot or serial tracking");
                    }
                }
                case LOT -> {
                    if (serial != null)
                        throw new BusinessException("TRACKING_MISMATCH", item.sku() + " is lot tracked");
                    if (lot == null) throw new BusinessException("LOT_REQUIRED", "Scan the lot of " + item.sku());
                }
                case SERIAL -> {
                    if (lot != null)
                        throw new BusinessException("TRACKING_MISMATCH", item.sku() + " is serial tracked");
                    if (serial == null)
                        throw new BusinessException("SERIAL_REQUIRED", "Scan the serial of " + item.sku());
                    if (a.quantity().compareTo(BigDecimal.ONE) != 0) {
                        throw new BusinessException("SERIAL_QTY_MUST_BE_ONE", "One unit per serial number");
                    }
                    if (!serials.add(serial)) {
                        throw new BusinessException("SERIAL_DUPLICATED", "Serial " + serial + " appears twice");
                    }
                }
            }

            // expired lots are refused unless a manager overrides; FEFO itself is not enforced
            boolean overridden = false;
            LocalDate expiry = lot == null ? null : expiries.get(new LotKey(a.locationId(), lot));
            if (expiry != null && expiry.isBefore(today)) {
                if (!expiryOverride) {
                    throw new BusinessException("LOT_EXPIRED", "Lot " + lot + " expired on " + expiry);
                }
                if (!privileged) {
                    throw new ForbiddenException("Only a manager can pick an expired lot");
                }
                overridden = true;
            }
            PickAllocation pa = new PickAllocation();
            pa.setLocationId(a.locationId());
            pa.setLotNumber(lot);
            pa.setSerialNumber(serial);
            pa.setQuantity(a.quantity());
            pa.setExpiryOverride(overridden);
            out.add(pa);
        }
        return out;
    }

    private Map<LotKey, LocalDate> loadExpiries(UUID warehouseId, UUID itemId) {
        Map<LotKey, LocalDate> map = new HashMap<>();
        for (StockLevelResponse s : inventoryQuery.findAvailable(warehouseId, itemId)) {
            if (s.lotNumber() != null && s.expiryDate() != null) {
                map.put(new LotKey(s.locationId(), s.lotNumber()), s.expiryDate());
            }
        }
        return map;
    }

    // ----------------------------------------------------------------- cancel

    /**
     * Manager action. Lines already picked keep their stock movement and stay shippable.
     */
    @Override
    public PickListResponse cancel(UUID pickListId, CancelPickListRequest r) {
        String reason = r == null ? null : Codes.blankToNull(r.reason());
        if (reason == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancel reason is required");
        }
        checkLength(reason, 500, "Reason");
        UUID orderId = load(pickListId).getSalesOrder().getId();
        orders.findByIdForUpdate(orderId).orElseThrow(() -> new NotFoundException("SalesOrder", orderId));
        PickList p = load(pickListId);                              // re-read under the order lock
        if (!PickListStatus.OPEN.contains(p.getStatus())) {
            throw new ConflictException("PICK_LIST_INVALID_STATE", "The pick list is " + p.getStatus());
        }
        cancelList(p, reason);
        return toResponses(List.of(lists.saveAndFlush(p))).get(0);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelOpenForOrder(UUID salesOrderId, String reason) {
        for (PickList p : lists.findBySalesOrderIdOrderByReleasedAtDesc(salesOrderId)) {
            if (PickListStatus.OPEN.contains(p.getStatus())) {
                cancelList(p, reason);
                lists.saveAndFlush(p);
            }
        }
    }

    private void cancelList(PickList p, String reason) {
        taskService.cancelByReference(OutboundReferences.PICK_LIST, p.getId(), reason);   // DONE task lines stay as they are
        for (PickLine l : p.getLines()) {
            if (l.getStatus() == PickLineStatus.PENDING) {
                l.setStatus(PickLineStatus.SHORT);
                l.setShortReason(reason);                            // satisfies ck_pick_lines_short_reason
                SalesOrderLine sol = l.getSalesOrderLine();
                sol.setReleasedQty(sol.getReleasedQty().subtract(l.getPlannedQty().subtract(l.getPickedQty())));
            }
        }
        p.setStatus(PickListStatus.CANCELLED);
        p.setCancelReason(reason);
    }

    // ---------------------------------------------------------------- helpers

    private PickList load(UUID id) {
        return lists.findById(id).orElseThrow(() -> new NotFoundException("PickList", id));
    }

    private static String normalizeOrNull(String s) {
        return s == null || s.isBlank() ? null : Codes.normalize(s);
    }

    private void checkLength(String text, int max, String field) {
        if (text != null && text.length() > max) {
            throw new BusinessException("TEXT_TOO_LONG", field + " is longer than " + max + " characters");
        }
    }

    /**
     * Batch lookups once for all the lists: items, locations (suggested bins and allocations) and customer names.
     */
    private List<PickListResponse> toResponses(List<PickList> pickLists) {
        Set<UUID> itemIds = new HashSet<>();
        Set<UUID> locationIds = new HashSet<>();
        for (PickList p : pickLists) {
            for (PickLine l : p.getLines()) {
                itemIds.add(l.getItemId());
                if (l.getSuggestedLocationId() != null) locationIds.add(l.getSuggestedLocationId());
                l.getAllocations().forEach(a -> locationIds.add(a.getLocationId()));
            }
        }
        Map<UUID, ItemResponse> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) items.findAllByIds(itemIds).forEach(i -> itemMap.put(i.id(), i));
        Map<UUID, LocationResponse> locationMap = new HashMap<>();
        if (!locationIds.isEmpty()) locations.findAllByIds(locationIds).forEach(l -> locationMap.put(l.id(), l));
        Map<UUID, String> names = new HashMap<>();
        return pickLists.stream().map(p -> PickListMapper.toResponse(p,
                names.computeIfAbsent(p.getSalesOrder().getCustomerId(), id -> customers.getById(id).name()),
                itemMap, locationMap)).toList();
    }
}

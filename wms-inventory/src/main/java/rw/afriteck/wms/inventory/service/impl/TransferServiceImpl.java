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
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockTransfer;
import rw.afriteck.wms.inventory.entity.StockTransferLine;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.inventory.mapper.TransferMapper;
import rw.afriteck.wms.inventory.repository.LotRepository;
import rw.afriteck.wms.inventory.repository.StockTransferRepository;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.inventory.service.TransferService;
import rw.afriteck.wms.inventory.support.*;
import rw.afriteck.wms.operations.dtos.CreateTaskLineRequest;
import rw.afriteck.wms.operations.dtos.CreateTaskRequest;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
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
public class TransferServiceImpl implements TransferService {

    private static final Set<String> SORTS = Set.of("createdAt", "transferNumber", "status");

    private final StockTransferRepository transfers;
    private final LotRepository lots;
    private final InventoryService inventory;
    private final TaskService taskService;
    private final WarehouseService warehouseService;
    private final ItemService itemService;
    private final LocationService locationService;
    private final Clock clock;

    // =============================== create

    @Override
    @Transactional
    public TransferResponse create(CreateTransferRequest r) {
        if (r == null || r.type() == null) {
            throw new BusinessException("TRANSFER_TYPE_REQUIRED", "Transfer type is required");
        }
        if (r.lines() == null || r.lines().isEmpty()) {
            throw new BusinessException("TRANSFER_NO_LINES", "At least one line is required");
        }
        boolean bin = r.type() == TransferType.BIN;
        UUID sourceId = r.sourceWarehouseId();
        UUID destId = bin ? sourceId : r.destinationWarehouseId();
        if (sourceId == null || destId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "Source and destination warehouse are required");
        }
        if (bin && r.destinationWarehouseId() != null && !r.destinationWarehouseId().equals(sourceId)) {
            throw new BusinessException("TRANSFER_WAREHOUSE_MISMATCH", "A bin transfer stays inside one warehouse");
        }
        if (!bin && sourceId.equals(destId)) {
            throw new BusinessException("TRANSFER_SAME_WAREHOUSE", "Use a bin transfer inside one warehouse");
        }
        if (!bin && r.immediate()) {
            throw new BusinessException("TRANSFER_IMMEDIATE_NOT_ALLOWED", "Inter-warehouse transfers are shipped and received");
        }
        requireActiveWarehouse(sourceId);
        requireActiveWarehouse(destId);

        Map<UUID, ItemResponse> items = itemsById(r.lines().stream()
                .map(CreateTransferLineRequest::itemId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locations = new HashMap<>();
        Map<String, Lot> lotCache = new HashMap<>();

        StockTransfer t = new StockTransfer();
        t.setType(r.type());
        t.setStatus(TransferStatus.PENDING);
        t.setSourceWarehouseId(sourceId);
        t.setDestinationWarehouseId(destId);
        t.setNotes(Codes.blankToNull(r.notes()));

        int lineNo = 1;
        for (CreateTransferLineRequest l : r.lines()) {
            ItemResponse item = l.itemId() == null ? null : items.get(l.itemId());
            if (item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item not found: " + l.itemId());
            }
            if (l.quantity() == null) {
                throw new BusinessException("QTY_INVALID", "Quantity is required");
            }
            String lotNumber = InventoryCodes.normalizeOrNull(l.lotNumber());
            String serial = InventoryCodes.normalizeOrNull(l.serialNumber());
            StockLineRules.check(MovementType.TRANSFER_OUT, item, l.quantity().negate(), lotNumber, null, serial);

            LocationResponse from = bin(l.fromLocationId(), locations);
            if (!from.warehouseId().equals(sourceId)) {
                throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Source bin " + from.code() + " is in another warehouse");
            }
            UUID toId = null;
            if (bin) {
                if (l.toLocationId() == null) {
                    throw new BusinessException("TRANSFER_TO_LOCATION_REQUIRED", "Destination bin is required");
                }
                LocationResponse to = bin(l.toLocationId(), locations);
                if (!to.warehouseId().equals(sourceId)) {
                    throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Destination bin " + to.code() + " is in another warehouse");
                }
                if (to.id().equals(from.id())) {
                    throw new BusinessException("TRANSFER_SAME_LOCATION", "Source and destination bin are the same");
                }
                toId = to.id();
            } else if (l.toLocationId() != null) {
                throw new BusinessException("TRANSFER_TO_LOCATION_NOT_ALLOWED",
                        "The destination bin is chosen when the goods are received");
            }

            StockTransferLine line = new StockTransferLine();
            line.setTransfer(t);
            line.setLineNo(lineNo++);
            line.setItemId(item.id());
            line.setLot(lotNumber == null ? null : lotCache.computeIfAbsent(item.id() + "|" + lotNumber,
                    k -> lots.findByItemIdAndLotNumber(item.id(), lotNumber).orElseThrow(() ->
                            new BusinessException("LOT_NOT_FOUND", "Lot " + lotNumber + " does not exist for item " + item.sku()))));
            line.setSerialNumber(serial);
            line.setFromLocationId(from.id());
            line.setToLocationId(toId);
            line.setQuantity(l.quantity());
            t.getLines().add(line);
        }

        // early feedback (advisory): can the planned quantities move today?
        inventory.check(new PostStockRequest(null, null, null, null, null, null,
                bin ? binLines(t.getLines(), StockTransferLine::getQuantity) : outLines(t.getLines())));

        t.setTransferNumber(InventoryNumbers.transfer(transfers.nextNumber()));
        transfers.saveAndFlush(t);                                    // ids exist from here on

        if (bin && r.immediate()) {
            post(t, binLines(t.getLines(), StockTransferLine::getQuantity), null);
            Instant now = clock.instant();
            t.getLines().forEach(l -> {
                l.setShippedQty(l.getQuantity());
                l.setReceivedQty(l.getQuantity());
            });
            t.setStatus(TransferStatus.COMPLETED);
            t.setShippedAt(now);
            t.setCompletedAt(now);
        } else {
            createTask(t, r.priority(), r.dueAt());
        }
        return toResponse(transfers.saveAndFlush(t));
    }

    // ============================ planned BIN transfer and Inter-Warehouse transfer: worker confirms a line

    @Override
    @Transactional
    public TransferResponse confirmLine(UUID id, UUID lineId, ConfirmTransferLineRequest r, UUID actorId, boolean privileged) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "A user is required for this operation");
        }
        StockTransfer t = load(id);
        if (t.getStatus() != TransferStatus.PENDING) {
            throw new ConflictException("TRANSFER_INVALID_STATE",
                    "Transfer " + t.getTransferNumber() + " is " + t.getStatus());
        }
        StockTransferLine line = findLine(t, lineId);
        if (isClosed(line)) {
            throw new ConflictException("TRANSFER_LINE_CLOSED", "Line " + line.getLineNo() + " is already closed");
        }

        BigDecimal qty = r == null ? null : r.quantity();
        if (qty == null || qty.signum() < 0 || qty.stripTrailingZeros().scale() > StockLineRules.SCALE) {
            throw new BusinessException("QTY_INVALID", "Quantity must be zero or more with at most 4 decimals");
        }
        if (qty.compareTo(line.getQuantity()) > 0) {
            throw new BusinessException("DONE_QTY_EXCEEDS_PLAN", "Quantity exceeds the planned " + line.getQuantity());
        }
        boolean isShort = qty.compareTo(line.getQuantity()) < 0;
        String shortReason = Codes.blankToNull(r.shortReason());
        if (isShort && shortReason == null) {
            throw new BusinessException("SHORT_REASON_REQUIRED", "A reason is required when moving less than planned");
        }

        TaskResponse task = openTask(t);
        TaskLineResponse taskLine = task.lines().stream()
                .filter(tl -> lineId.equals(tl.referenceLineId())).findFirst()
                .orElseThrow(() -> new ConflictException("TRANSFER_TASK_NOT_FOUND", "No task line for this transfer line"));
        if (task.status() != TaskStatus.ASSIGNED && task.status() != TaskStatus.IN_PROGRESS) {
            throw new ConflictException("TRANSFER_TASK_NOT_STARTED", "Claim or assign the task before confirming lines");
        }
        if (!privileged && !actorId.equals(task.assigneeId())) {
            throw new ForbiddenException("This task is assigned to another user");
        }

        boolean bin = t.getType() == TransferType.BIN;
        if (qty.signum() > 0) {                                           // stock first, then the task line
            post(t, bin ? List.of(outLine(line, qty), inLine(line.getToLocationId(), line, qty))
                    : List.of(outLine(line, qty)), null);
        }
        line.setShippedQty(qty);
        if (bin) {
            line.setReceivedQty(qty);
        }
        line.setShortReason(isShort ? shortReason : null);
        taskService.completeLine(task.id(), taskLine.id(), qty, isShort ? shortReason : null);

        if (t.getLines().stream().allMatch(TransferServiceImpl::isClosed)) {
            Instant now = clock.instant();
            if (bin) {
                t.setStatus(TransferStatus.COMPLETED);
                t.setShippedAt(now);
                t.setCompletedAt(now);
            } else if (t.getLines().stream().anyMatch(l -> l.getShippedQty().signum() > 0)) {
                startTransit(t);
            } else {
                t.setStatus(TransferStatus.CANCELLED);
                t.setCancelReason("Nothing could be picked");
                t.setCompletedAt(now);
            }
        }
        return toResponse(transfers.saveAndFlush(t));
    }
    // =============================== INTER_WAREHOUSE: ship / receive

    @Override
    @Transactional
    public TransferResponse ship(UUID id) {
        StockTransfer t = load(id);
        requireInter(t, TransferStatus.PENDING);
        requireActiveWarehouse(t.getDestinationWarehouseId());
        taskService.cancelByReference(InventoryReferences.STOCK_TRANSFER, t.getId(), "Shipped by a manager without picking");
        List<StockTransferLine> open = t.getLines().stream().filter(l -> !isClosed(l)).toList();
        if (!open.isEmpty()) {                                            // lines already picked are already posted
            post(t, open.stream().map(l -> outLine(l, l.getQuantity())).toList(), null);
            open.forEach(l -> l.setShippedQty(l.getQuantity()));
        }
        startTransit(t);
        return toResponse(transfers.saveAndFlush(t));
    }

    @Override
    @Transactional
    public TransferResponse receive(UUID id, ReceiveTransferRequest r) {
        StockTransfer t = load(id);
        requireInter(t, TransferStatus.IN_TRANSIT);
        boolean hasLines = r != null && r.lines() != null && !r.lines().isEmpty();
        boolean closeShort = r != null && r.closeShort();
        if (!hasLines && !closeShort) {
            throw new BusinessException("TRANSFER_NOTHING_TO_RECEIVE", "Nothing to receive");
        }

        List<PostStockLine> in = new ArrayList<>();
        if (hasLines) {
            Map<UUID, LocationResponse> locations = new HashMap<>();
            for (ReceiveLineRequest rl : r.lines()) {
                StockTransferLine line = findLine(t, rl.lineId());
                BigDecimal qty = rl.quantity();
                if (qty == null || qty.signum() <= 0 || qty.stripTrailingZeros().scale() > StockLineRules.SCALE) {
                    throw new BusinessException("QTY_INVALID", "Received quantity must be above zero with at most 4 decimals");
                }
                BigDecimal remaining = line.getShippedQty().subtract(line.getReceivedQty());
                if (qty.compareTo(remaining) > 0) {
                    throw new BusinessException("RECEIVE_QTY_EXCEEDS_TRANSIT",
                            "Line " + line.getLineNo() + ": only " + remaining.stripTrailingZeros().toPlainString() + " in transit");
                }
                LocationResponse to = bin(rl.toLocationId(), locations);
                if (!to.warehouseId().equals(t.getDestinationWarehouseId())) {
                    throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Bin " + to.code() + " is not in the destination warehouse");
                }
                if (line.getToLocationId() != null && !line.getToLocationId().equals(to.id())) {
                    throw new BusinessException("TRANSFER_DESTINATION_MISMATCH",
                            "Line " + line.getLineNo() + " is already being received into another bin");
                }
                line.setToLocationId(to.id());
                line.setReceivedQty(line.getReceivedQty().add(qty));
                in.add(inLine(to.id(), line, qty));
            }
            post(t, in, null);
        }

        boolean allReceived = t.getLines().stream().allMatch(l -> l.getReceivedQty().compareTo(l.getShippedQty()) >= 0);
        if (!allReceived && closeShort) {
            String reason = Codes.blankToNull(r.shortReason());
            if (reason == null) {
                throw new BusinessException("SHORT_REASON_REQUIRED", "A reason is required to close the transfer short");
            }
            t.getLines().stream()
                    .filter(l -> l.getReceivedQty().compareTo(l.getShippedQty()) < 0)
                    .forEach(l -> l.setShortReason(reason));
        }
        if (allReceived || closeShort) {
            t.setStatus(TransferStatus.COMPLETED);
            t.setCompletedAt(clock.instant());
        }
        return toResponse(transfers.saveAndFlush(t));
    }

    // ========================== cancel

    @Override
    @Transactional
    public TransferResponse cancel(UUID id, String reason) {
        String text = Codes.blankToNull(reason);
        if (text == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancellation reason is required");
        }
        StockTransfer t = load(id);
        switch (t.getStatus()) {
            case PENDING -> {
                taskService.cancelByReference(InventoryReferences.STOCK_TRANSFER, t.getId(), text);   // closed lines stay closed
                if (t.getType() == TransferType.INTER_WAREHOUSE) {
                    returnShipped(t, "Cancelled transfer: picked stock returned to source");
                }
            }
            case IN_TRANSIT -> {
                if (t.getLines().stream().anyMatch(l -> l.getReceivedQty().signum() > 0)) {
                    throw new ConflictException("TRANSFER_PARTIALLY_RECEIVED",
                            "Goods were already received: receive the rest or close the transfer short");
                }
                returnShipped(t, "Cancelled transfer: stock returned to source");
            }
            default -> throw new ConflictException("TRANSFER_INVALID_STATE",
                    "Transfer " + t.getTransferNumber() + " is " + t.getStatus());
        }
        t.setStatus(TransferStatus.CANCELLED);
        t.setCancelReason(text);
        t.setCompletedAt(clock.instant());
        return toResponse(transfers.saveAndFlush(t));
    }

    // ============================ reads

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TransferAccessInfo accessInfo(UUID id) {
        return transfers.findById(id)
                .map(t -> new TransferAccessInfo(t.getSourceWarehouseId(), t.getDestinationWarehouseId(), t.getType()))
                .orElseThrow(() -> new NotFoundException("StockTransfer", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransferSummaryResponse> search(TransferFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        Set<TransferStatus> statuses = (f.statuses() == null || f.statuses().isEmpty())
                ? EnumSet.allOf(TransferStatus.class) : f.statuses();
        Pageable p = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransfer> page = transfers.search(f.warehouseId(), statuses, Codes.likePattern(f.q()), p);

        Map<UUID, Integer> counts = new HashMap<>();
        List<UUID> ids = page.getContent().stream().map(StockTransfer::getId).toList();
        if (!ids.isEmpty()) {
            transfers.countLines(ids).forEach(c -> counts.put(c.getTransferId(), (int) c.getLineCount()));
        }
        return PageResponse.of(page.map(t -> TransferMapper.toSummary(t, counts.getOrDefault(t.getId(), 0))));
    }

    // ============================== helpers

    private void post(StockTransfer t, List<PostStockLine> lines, String note) {
        inventory.post(new PostStockRequest(t.getId(), null, InventoryReferences.STOCK_TRANSFER,
                t.getId(), t.getTransferNumber(), note, lines));
    }

    private static PostStockLine outLine(StockTransferLine l, BigDecimal qty) {
        return new PostStockLine(MovementType.TRANSFER_OUT, l.getFromLocationId(), l.getItemId(),
                lotNumberOf(l), null, l.getSerialNumber(), qty.negate(), null);
    }

    private static PostStockLine inLine(UUID locationId, StockTransferLine l, BigDecimal qty) {
        return new PostStockLine(MovementType.TRANSFER_IN, locationId, l.getItemId(),
                lotNumberOf(l), null, l.getSerialNumber(), qty, null);
    }

    private static List<PostStockLine> outLines(List<StockTransferLine> lines) {
        return lines.stream().map(l -> outLine(l, l.getQuantity())).toList();
    }

    /**
     * OUT then IN for each line, so a serial leaves its bin before it arrives in the next.
     */
    private static List<PostStockLine> binLines(List<StockTransferLine> lines, Function<StockTransferLine, BigDecimal> qty) {
        List<PostStockLine> result = new ArrayList<>(lines.size() * 2);
        for (StockTransferLine l : lines) {
            result.add(outLine(l, qty.apply(l)));
            result.add(inLine(l.getToLocationId(), l, qty.apply(l)));
        }
        return result;
    }

    private static String lotNumberOf(StockTransferLine l) {
        return l.getLot() == null ? null : l.getLot().getLotNumber();
    }

    /**
     * A line is closed when fully moved, or closed short with a reason.
     */
    private static boolean isClosed(StockTransferLine l) {
        return l.getShortReason() != null || l.getShippedQty().compareTo(l.getQuantity()) >= 0;
    }

    private void createTask(StockTransfer t, TaskPriority priority, Instant dueAt) {
        List<CreateTaskLineRequest> lines = t.getLines().stream()
                .map(l -> new CreateTaskLineRequest(l.getItemId(), lotNumberOf(l), l.getSerialNumber(),
                        l.getFromLocationId(), l.getToLocationId(), l.getQuantity(), l.getId()))
                .toList();
        taskService.create(new CreateTaskRequest(TaskType.TRANSFER, priority, t.getSourceWarehouseId(), null,
                InventoryReferences.STOCK_TRANSFER, t.getId(), t.getTransferNumber(), dueAt, t.getNotes(), lines));
    }

    private TaskResponse openTask(StockTransfer t) {
        return taskService.findByReference(InventoryReferences.STOCK_TRANSFER, t.getId()).stream()
                .filter(task -> TaskStatus.ACTIVE.contains(task.status()))
                .findFirst()
                .orElseThrow(() -> new ConflictException("TRANSFER_TASK_NOT_FOUND", "No open task for this transfer"));
    }

    private LocationResponse bin(UUID id, Map<UUID, LocationResponse> cache) {
        if (id == null) {
            throw new BusinessException("LOCATION_NOT_FOUND", "Location is required");
        }
        return cache.computeIfAbsent(id, locationService::requireStorableBin);
    }

    private void requireActiveWarehouse(UUID id) {
        if (!warehouseService.getById(id).active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
    }

    private static void requireInter(StockTransfer t, TransferStatus expected) {
        if (t.getType() != TransferType.INTER_WAREHOUSE || t.getStatus() != expected) {
            throw new ConflictException("TRANSFER_INVALID_STATE",
                    "Transfer " + t.getTransferNumber() + " is " + t.getType() + " / " + t.getStatus());
        }
    }

    private static StockTransferLine findLine(StockTransfer t, UUID lineId) {
        return t.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
                .orElseThrow(() -> new NotFoundException("StockTransferLine", lineId));
    }

    private StockTransfer load(UUID id) {
        return transfers.findWithLinesById(id).orElseThrow(() -> new NotFoundException("StockTransfer", id));
    }

    private TransferResponse toResponse(StockTransfer t) {
        Set<UUID> itemIds = t.getLines().stream().map(StockTransferLine::getItemId).collect(Collectors.toSet());
        Set<UUID> locationIds = new HashSet<>();
        t.getLines().forEach(l -> {
            locationIds.add(l.getFromLocationId());
            if (l.getToLocationId() != null) locationIds.add(l.getToLocationId());
        });
        return TransferMapper.toResponse(t, itemsById(itemIds), locationsById(locationIds));
    }

    private Map<UUID, ItemResponse> itemsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return itemService.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    private Map<UUID, LocationResponse> locationsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return locationService.findAllByIds(ids).stream().collect(Collectors.toMap(LocationResponse::id, Function.identity()));
    }

    private void startTransit(StockTransfer t) {
        t.setStatus(TransferStatus.IN_TRANSIT);
        t.setShippedAt(clock.instant());
    }

    /**
     * Puts shipped stock back into the original source bins (inter-warehouse cancel).
     */
    private void returnShipped(StockTransfer t, String note) {
        List<PostStockLine> back = t.getLines().stream()
                .filter(l -> l.getShippedQty().signum() > 0)
                .map(l -> inLine(l.getFromLocationId(), l, l.getShippedQty()))
                .toList();
        if (!back.isEmpty()) {
            post(t, back, note);
        }
    }
}

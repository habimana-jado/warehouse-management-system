package rw.afriteck.wms.inbound.service.impl;

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
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.entity.GoodsReceipt;
import rw.afriteck.wms.inbound.entity.GoodsReceiptLine;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.enums.PutawayStatus;
import rw.afriteck.wms.inbound.enums.ReceiptMode;
import rw.afriteck.wms.inbound.mapper.GoodsReceiptMapper;
import rw.afriteck.wms.inbound.repository.GoodsReceiptLineRepository;
import rw.afriteck.wms.inbound.repository.GoodsReceiptRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
import rw.afriteck.wms.inbound.support.InboundNumbers;
import rw.afriteck.wms.inbound.support.InboundReferences;
import rw.afriteck.wms.inbound.support.PutawaySuggester;
import rw.afriteck.wms.inbound.support.TrackingRules;
import rw.afriteck.wms.inventory.dtos.PostStockLine;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.operations.dtos.CreateTaskLineRequest;
import rw.afriteck.wms.operations.dtos.CreateTaskRequest;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("receiptNumber", "receivedAt", "mode");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "receivedAt");

    private final PurchaseOrderRepository purchaseOrders;
    private final GoodsReceiptRepository goodsReceipts;
    private final GoodsReceiptLineRepository receiptLines;
    private final WarehouseService warehouses;
    private final LocationService locations;
    private final ItemService items;
    private final InventoryService inventory;
    private final TaskService tasks;
    private final PutawaySuggester suggester;
    private final WorkerLookup workers;
    private final Clock clock;

    /** One validated request line, before anything is written. */
    private static final class Prep {
        int lineNo;
        PurchaseOrderLine poLine;
        ItemResponse item;
        String lot;
        LocalDate expiry;
        String serial;
        BigDecimal accepted;
        BigDecimal rejected;
        String rejectReason;
        UUID requestedLocationId;     // DIRECT mode only
        UUID acceptLocationId;        // RECEIVING bin (PUTAWAY) or STORAGE bin (DIRECT)
    }

    // ================================================================== receive

    @Override
    @Transactional
    public GoodsReceiptResponse receive(UUID purchaseOrderId, ReceiveGoodsRequest r, UUID actorId) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "Receiving needs a user");
        }
        if (r == null || r.lines() == null || r.lines().isEmpty()) {
            throw new BusinessException("RECEIPT_NO_LINES", "A receipt needs at least one line");
        }

        PurchaseOrder po = purchaseOrders.findByIdForUpdate(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("PurchaseOrder", purchaseOrderId));
        if (!PurchaseOrderStatus.RECEIVABLE.contains(po.getStatus())) {
            throw new ConflictException("PO_INVALID_STATE",
                    "Goods can only be received on a CONFIRMED or PARTIALLY_RECEIVED purchase order (current status: "
                            + po.getStatus() + ")");
        }
        UUID warehouseId = po.getWarehouseId();
        WarehouseResponse warehouse = warehouses.getById(warehouseId);
        if (!warehouse.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse " + warehouse.name() + " is inactive");
        }
        ReceiptMode mode = r.mode() == null ? ReceiptMode.PUTAWAY : r.mode();

        List<Prep> preps = prepare(po, r);
        UUID damagedBin = resolveLocations(warehouseId, mode, r, preps);

        // ---- the receipt document
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptNumber(InboundNumbers.goodsReceipt(goodsReceipts.nextReceiptNumber()));
        receipt.setPurchaseOrder(po);
        receipt.setWarehouseId(warehouseId);
        receipt.setMode(mode);
        receipt.setReceivedAt(clock.instant());
        receipt.setReceivedBy(actorId);
        receipt.setDeliveryNoteNumber(Codes.blankToNull(r.deliveryNoteNumber()));
        receipt.setNotes(Codes.blankToNull(r.notes()));

        for (Prep p : preps) {
            GoodsReceiptLine line = new GoodsReceiptLine();
            line.setLineNo(p.lineNo);
            line.setPurchaseOrderLine(p.poLine);
            line.setItemId(p.item.id());
            line.setLotNumber(p.lot);
            line.setExpiryDate(p.expiry);
            line.setSerialNumber(p.serial);
            line.setAcceptedQty(p.accepted);
            line.setRejectedQty(p.rejected);
            line.setRejectReason(p.rejected.signum() > 0 ? p.rejectReason : null);
            line.setReceivingLocationId(p.accepted.signum() > 0 ? p.acceptLocationId : null);
            line.setRejectLocationId(p.rejected.signum() > 0 ? damagedBin : null);
            line.setUnitCost(p.poLine.getUnitPrice());        // PO price per base unit; null = unknown
            line.setPutawayStatus(PutawayStatus.NOT_REQUIRED);
            receipt.addLine(line);
        }
        goodsReceipts.saveAndFlush(receipt);                  // the id is needed as the ledger reference

        // ---- stock: accepted into the receiving/storage bin, rejected into the DAMAGED bin
        List<PostStockLine> stockLines = new ArrayList<>();
        for (GoodsReceiptLine l : receipt.getLines()) {
            if (l.getAcceptedQty().signum() > 0) {
                stockLines.add(PostStockLine.receipt(l.getReceivingLocationId(), l.getItemId(), l.getLotNumber(),
                        l.getExpiryDate(), l.getSerialNumber(), l.getAcceptedQty(), l.getUnitCost()));
            }
            if (l.getRejectedQty().signum() > 0) {
                stockLines.add(PostStockLine.receipt(l.getRejectLocationId(), l.getItemId(), l.getLotNumber(),
                        l.getExpiryDate(), l.getSerialNumber(), l.getRejectedQty(), l.getUnitCost()));
            }
        }
        inventory.post(new PostStockRequest(
                receipt.getId(),                              // transactionId: the document id
                receipt.getReceivedAt(),
                InboundReferences.GOODS_RECEIPT,
                receipt.getId(),
                receipt.getReceiptNumber(),
                postingNote(po, receipt),
                stockLines));

        // ---- PO totals and status
        for (Prep p : preps) {
            p.poLine.setAcceptedQty(p.poLine.getAcceptedQty().add(p.accepted));
            p.poLine.setRejectedQty(p.poLine.getRejectedQty().add(p.rejected));
        }
        po.setStatus(po.getLines().stream().allMatch(PurchaseOrderLine::isFulfilled)
                ? PurchaseOrderStatus.RECEIVED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED);

        // ---- put-away task
        if (mode == ReceiptMode.PUTAWAY) {
            createPutawayTask(receipt);
        }

        goodsReceipts.saveAndFlush(receipt);
        purchaseOrders.saveAndFlush(po);
        return toResponse(receipt);
    }

    /** Validates every line and converts units. Nothing is written here. */
    private List<Prep> prepare(PurchaseOrder po, ReceiveGoodsRequest r) {
        Map<UUID, PurchaseOrderLine> poLines = po.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));

        Set<UUID> itemIds = new HashSet<>();
        for (ReceiveGoodsLineRequest req : r.lines()) {
            PurchaseOrderLine pl = req.purchaseOrderLineId() == null ? null : poLines.get(req.purchaseOrderLineId());
            if (pl == null) {
                throw new BusinessException("PO_LINE_NOT_FOUND", "Line " + req.purchaseOrderLineId() + " is not on this purchase order");
            }
            itemIds.add(pl.getItemId());
        }
        Map<UUID, ItemResponse> itemMap = items.findAllByIds(itemIds).stream()
                .collect(Collectors.toMap(ItemResponse::id, Function.identity()));

        List<Prep> preps = new ArrayList<>();
        Map<UUID, BigDecimal> acceptedPerPoLine = new LinkedHashMap<>();
        Set<String> serialsSeen = new HashSet<>();
        int lineNo = 0;

        for (ReceiveGoodsLineRequest req : r.lines()) {
            Prep p = new Prep();
            p.lineNo = ++lineNo;
            p.poLine = poLines.get(req.purchaseOrderLineId());
            p.item = itemMap.get(p.poLine.getItemId());
            if (p.item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item " + p.poLine.getItemId() + " does not exist");
            }
            if (!p.item.active()) {
                throw new BusinessException("ITEM_INACTIVE", "Item " + p.item.sku() + " is inactive");
            }

            p.accepted = toBase(p.item, req.uom(), req.acceptedQty());
            p.rejected = toBase(p.item, req.uom(), req.rejectedQty());
            if (p.accepted.signum() <= 0 && p.rejected.signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Line " + p.lineNo + " (" + p.item.sku() + ") has no quantity");
            }
            p.rejectReason = Codes.blankToNull(req.rejectReason());
            if (p.rejected.signum() > 0 && p.rejectReason == null) {
                throw new BusinessException("REJECT_REASON_REQUIRED",
                        "A reason is required for the rejected quantity of " + p.item.sku());
            }

            p.lot = normalizeOrNull(req.lotNumber());
            p.serial = normalizeOrNull(req.serialNumber());
            p.expiry = req.expiryDate();
            TrackingRules.check(p.item, p.lot, p.expiry, p.serial, p.accepted.add(p.rejected));

            if (p.serial != null && !serialsSeen.add(p.item.id() + "|" + p.serial)) {
                throw new BusinessException("SERIAL_DUPLICATED", "Serial " + p.serial + " appears twice in this receipt");
            }

            p.requestedLocationId = req.locationId();
            acceptedPerPoLine.merge(p.poLine.getId(), p.accepted, BigDecimal::add);
            preps.add(p);
        }

        // tolerance: total accepted per PO line (several request lines may hit the same PO line)
        for (Map.Entry<UUID, BigDecimal> e : acceptedPerPoLine.entrySet()) {
            PurchaseOrderLine pl = poLines.get(e.getKey());
            BigDecimal left = pl.acceptableQtyLeft();
            if (e.getValue().compareTo(left) > 0) {
                ItemResponse item = itemMap.get(pl.getItemId());
                throw new BusinessException("RECEIPT_EXCEEDS_TOLERANCE",
                        "Item " + item.sku() + ": at most " + left.stripTrailingZeros().toPlainString()
                                + " more can be accepted (ordered " + pl.getOrderedQty().stripTrailingZeros().toPlainString()
                                + ", accepted " + pl.getAcceptedQty().stripTrailingZeros().toPlainString()
                                + ", tolerance " + pl.getTolerancePercent().stripTrailingZeros().toPlainString() + "%)");
            }
        }
        return preps;
    }

    /**
     * Resolves and validates the bins. Returns the DAMAGED bin (null when nothing is rejected).
     * A bin is only required when stock is posted into it.
     */
    private UUID resolveLocations(UUID warehouseId, ReceiptMode mode, ReceiveGoodsRequest r, List<Prep> preps) {
        boolean anyAccepted = preps.stream().anyMatch(p -> p.accepted.signum() > 0);
        boolean anyRejected = preps.stream().anyMatch(p -> p.rejected.signum() > 0);

        if (anyAccepted) {
            if (mode == ReceiptMode.PUTAWAY) {
                UUID receivingBin = r.receivingLocationId() != null
                        ? checkedBin(r.receivingLocationId(), warehouseId, LocationUsage.RECEIVING)
                        : firstBin(warehouseId, LocationUsage.RECEIVING, "NO_RECEIVING_LOCATION",
                        "The warehouse has no active RECEIVING bin");
                preps.stream().filter(p -> p.accepted.signum() > 0).forEach(p -> p.acceptLocationId = receivingBin);
            } else {
                // DIRECT: each accepted line goes straight to a STORAGE bin
                Set<UUID> needSuggestion = new LinkedHashSet<>();
                for (Prep p : preps) {
                    if (p.accepted.signum() > 0) {
                        if (p.requestedLocationId != null) {
                            p.acceptLocationId = checkedBin(p.requestedLocationId, warehouseId, LocationUsage.STORAGE);
                        } else {
                            needSuggestion.add(p.item.id());
                        }
                    }
                }
                if (!needSuggestion.isEmpty()) {
                    Map<UUID, UUID> suggestions = suggester.suggest(warehouseId, needSuggestion);
                    for (Prep p : preps) {
                        if (p.accepted.signum() > 0 && p.acceptLocationId == null) {
                            p.acceptLocationId = suggestions.get(p.item.id());
                            if (p.acceptLocationId == null) {
                                throw new BusinessException("NO_STORAGE_LOCATION", "The warehouse has no active STORAGE bin");
                            }
                        }
                    }
                }
            }
        }

        if (!anyRejected) {
            return null;
        }
        return r.damagedLocationId() != null
                ? checkedBin(r.damagedLocationId(), warehouseId, LocationUsage.DAMAGED)
                : firstBin(warehouseId, LocationUsage.DAMAGED, "NO_DAMAGED_LOCATION",
                "The warehouse has no active DAMAGED bin for rejected goods");
    }

    private UUID checkedBin(UUID id, UUID warehouseId, LocationUsage usage) {
        LocationResponse loc = locations.requireStorableBin(id);       // BusinessException unless an active BIN
        if (!warehouseId.equals(loc.warehouseId())) {
            throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Location " + loc.code() + " belongs to another warehouse");
        }
        if (loc.usage() != usage) {
            throw new BusinessException("LOCATION_USAGE_INVALID", "Location " + loc.code() + " must be a " + usage + " bin");
        }
        return loc.id();
    }

    private UUID firstBin(UUID warehouseId, LocationUsage usage, String errorCode, String message) {
        return locations.findBinsByUsage(warehouseId, usage).stream()      // active bins only
                .min(Comparator.comparing(LocationResponse::code))
                .map(LocationResponse::id)
                .orElseThrow(() -> new BusinessException(errorCode, message));
    }

    private void createPutawayTask(GoodsReceipt receipt) {
        List<GoodsReceiptLine> toMove = receipt.getLines().stream()
                .filter(l -> l.getAcceptedQty().signum() > 0)
                .toList();
        if (toMove.isEmpty()) {
            return;                                                         // everything was rejected: nothing to put away
        }
        Set<UUID> itemIds = toMove.stream().map(GoodsReceiptLine::getItemId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, UUID> suggestions = suggester.suggest(receipt.getWarehouseId(), itemIds);

        List<CreateTaskLineRequest> taskLines = new ArrayList<>();
        for (GoodsReceiptLine l : toMove) {
            UUID to = suggestions.get(l.getItemId());                       // may be null: the worker picks the bin
            l.setSuggestedLocationId(to);
            taskLines.add(new CreateTaskLineRequest(
                    l.getItemId(), l.getLotNumber(), l.getSerialNumber(),
                    l.getReceivingLocationId(), to,
                    l.getAcceptedQty(),
                    l.getId()));                                            // referenceLineId = receipt line id
        }
        TaskResponse task = tasks.create(new CreateTaskRequest(
                TaskType.PUTAWAY, null, receipt.getWarehouseId(), null,
                InboundReferences.GOODS_RECEIPT, receipt.getId(), receipt.getReceiptNumber(),
                null, null, taskLines));

        receipt.setTaskId(task.id());
        Map<UUID, UUID> taskLineByReceiptLine = task.lines().stream()
                .collect(Collectors.toMap(TaskLineResponse::referenceLineId, TaskLineResponse::id));
        for (GoodsReceiptLine l : toMove) {
            l.setTaskLineId(taskLineByReceiptLine.get(l.getId()));
            l.setPutawayStatus(PutawayStatus.PENDING);
        }
    }

    // ================================================================== put-away confirmation

    @Override
    @Transactional
    public GoodsReceiptResponse confirmPutaway(UUID receiptId, UUID lineId, ConfirmPutawayRequest r,
                                               UUID actorId, boolean privileged) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "Put-away needs a user");
        }
        GoodsReceiptLine line = receiptLines.findByIdForUpdate(lineId)
                .orElseThrow(() -> new NotFoundException("GoodsReceiptLine", lineId));
        GoodsReceipt receipt = line.getGoodsReceipt();
        if (!receipt.getId().equals(receiptId)) {
            throw new NotFoundException("GoodsReceiptLine", lineId);
        }
        if (line.getPutawayStatus() != PutawayStatus.PENDING) {
            throw new ConflictException("PUTAWAY_LINE_CLOSED", "This line is not waiting for put-away (status "
                    + line.getPutawayStatus() + ")");
        }

        // the task must be handed to this worker (or the caller must be a manager) and started or claimed
        TaskResponse task = tasks.getById(receipt.getTaskId());
        if (task.status() != TaskStatus.ASSIGNED && task.status() != TaskStatus.IN_PROGRESS) {
            throw new ConflictException("PUTAWAY_TASK_NOT_STARTED",
                    "The put-away task must be assigned and started first (current status: " + task.status() + ")");
        }
        if (!privileged && !actorId.equals(task.assigneeId())) {
            throw new ForbiddenException("This put-away task is assigned to another user");
        }

        BigDecimal planned = line.getAcceptedQty();
        BigDecimal done = (r == null || r.doneQty() == null) ? planned : r.doneQty().setScale(4, RoundingMode.HALF_UP);
        if (done.signum() < 0) {
            throw new BusinessException("QTY_INVALID", "The quantity cannot be negative");
        }
        if (done.compareTo(planned) > 0) {
            throw new BusinessException("DONE_QTY_EXCEEDS_PLAN", "More than the accepted quantity cannot be put away");
        }
        String shortReason = r == null ? null : Codes.blankToNull(r.shortReason());
        if (done.compareTo(planned) < 0 && shortReason == null) {
            throw new BusinessException("SHORT_REASON_REQUIRED", "A reason is required when less than planned is put away");
        }

        UUID destination = null;
        if (done.signum() > 0) {
            UUID wanted = (r != null && r.locationId() != null) ? r.locationId() : line.getSuggestedLocationId();
            if (wanted == null) {
                throw new BusinessException("PUTAWAY_LOCATION_REQUIRED", "Choose the bin the goods were put into");
            }
            destination = checkedBin(wanted, receipt.getWarehouseId(), LocationUsage.STORAGE);

            // ledger first, then the task line (backbone rule); both roll back together
            inventory.post(new PostStockRequest(
                    null,                                               // generated: groups the two lines of this move
                    null,
                    InboundReferences.GOODS_RECEIPT,
                    receipt.getId(),
                    receipt.getReceiptNumber(),
                    "Put-away",
                    List.of(
                            new PostStockLine(MovementType.TRANSFER_OUT, line.getReceivingLocationId(), line.getItemId(),
                                    line.getLotNumber(), null, line.getSerialNumber(), done.negate(), null),
                            new PostStockLine(MovementType.TRANSFER_IN, destination, line.getItemId(),
                                    line.getLotNumber(), line.getExpiryDate(), line.getSerialNumber(), done, null))));
        }

        line.setPutawayDoneQty(done);
        line.setPutawayLocationId(destination);
        line.setPutawayStatus(done.compareTo(planned) == 0 ? PutawayStatus.DONE : PutawayStatus.SHORT);
        receiptLines.saveAndFlush(line);

        tasks.completeLine(receipt.getTaskId(), line.getTaskLineId(), done, shortReason);
        return toResponse(receipt);
    }

    // ================================================================== queries

    @Override
    public GoodsReceiptResponse getById(UUID id) {
        GoodsReceipt receipt = goodsReceipts.findWithLinesById(id)
                .orElseThrow(() -> new NotFoundException("GoodsReceipt", id));
        return toResponse(receipt);
    }

    @Override
    public List<GoodsReceiptSummaryResponse> findByPurchaseOrder(UUID purchaseOrderId) {
        List<GoodsReceipt> content = goodsReceipts.findByPurchaseOrderIdOrderByReceivedAtDesc(purchaseOrderId);
        return content.stream().map(summaryMapper(content)).toList();
    }

    @Override
    public PageResponse<GoodsReceiptSummaryResponse> search(UUID warehouseId, String q, Pageable pageable) {
        requireWarehouse(warehouseId);
        String pattern = Codes.blankToNull(q) == null ? "%" : Codes.likePattern(q).toLowerCase(Locale.ROOT);
        Pageable safe = PageRules.sanitize(pageable, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<GoodsReceipt> page = goodsReceipts.search(warehouseId, pattern, safe);
        return PageResponse.of(page.map(summaryMapper(page.getContent())));
    }

    @Override
    public PageResponse<GoodsReceiptSummaryResponse> awaitingPutaway(UUID warehouseId, Pageable pageable) {
        requireWarehouse(warehouseId);
        Pageable safe = PageRules.sanitize(pageable, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<GoodsReceipt> page = goodsReceipts.findWithLinesInPutawayStatus(warehouseId, PutawayStatus.PENDING, safe);
        return PageResponse.of(page.map(summaryMapper(page.getContent())));
    }

    @Override
    public UUID warehouseIdOf(UUID receiptId) {
        return goodsReceipts.findById(receiptId)
                .map(GoodsReceipt::getWarehouseId)
                .orElseThrow(() -> new NotFoundException("GoodsReceipt", receiptId));
    }

    // ================================================================== helpers

    private static void requireWarehouse(UUID warehouseId) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
    }

    /** Quantity in `uom` to the item's base UOM; null or zero is ZERO. */
    private BigDecimal toBase(ItemResponse item, String uom, BigDecimal qty) {
        if (qty == null || qty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (qty.signum() < 0) {
            throw new BusinessException("QTY_INVALID", "Quantities cannot be negative (item " + item.sku() + ")");
        }
        String u = Codes.blankToNull(uom);
        BigDecimal base = (u == null || u.equalsIgnoreCase(item.baseUom()))
                ? qty
                : items.toBaseQuantity(item.id(), Codes.normalize(u), qty);
        return base.setScale(4, RoundingMode.HALF_UP);
    }

    /** Lot and serial numbers are normalized exactly as operations and inventory store them. */
    private static String normalizeOrNull(String raw) {
        String v = Codes.blankToNull(raw);
        return v == null ? null : Codes.normalize(v);
    }

    private static String postingNote(PurchaseOrder po, GoodsReceipt receipt) {
        String note = "PO " + po.getPoNumber();
        if (receipt.getDeliveryNoteNumber() != null) {
            note += ", delivery note " + receipt.getDeliveryNoteNumber();
        }
        return note;
    }

    private GoodsReceiptResponse toResponse(GoodsReceipt receipt) {
        Set<UUID> itemIds = receipt.getLines().stream().map(GoodsReceiptLine::getItemId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> itemMap = itemIds.isEmpty()
                ? Map.of()
                : items.findAllByIds(itemIds).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));

        Set<UUID> locationIds = new HashSet<>();
        for (GoodsReceiptLine l : receipt.getLines()) {
            addIfNotNull(locationIds, l.getReceivingLocationId());
            addIfNotNull(locationIds, l.getRejectLocationId());
            addIfNotNull(locationIds, l.getSuggestedLocationId());
            addIfNotNull(locationIds, l.getPutawayLocationId());
        }
        Map<UUID, String> locationCodes = locationIds.isEmpty()
                ? Map.of()
                : locations.findAllByIds(locationIds).stream()
                .collect(Collectors.toMap(LocationResponse::id, LocationResponse::code));

        Map<UUID, String> userNames = workers.findDisplayNames(List.of(receipt.getReceivedBy()));
        return GoodsReceiptMapper.toResponse(receipt, receipt.getPurchaseOrder().getPoNumber(),
                itemMap, locationCodes, userNames);
    }

    private static void addIfNotNull(Set<UUID> set, UUID id) {
        if (id != null) {
            set.add(id);
        }
    }

    /** Resolves everything a list screen needs in batch, then returns a cheap per-row mapper. */
    private Function<GoodsReceipt, GoodsReceiptSummaryResponse> summaryMapper(Collection<GoodsReceipt> content) {
        if (content.isEmpty()) {
            return r -> null;
        }
        List<UUID> ids = content.stream().map(GoodsReceipt::getId).toList();
        Map<UUID, Integer> lineCounts = toCountMap(receiptLines.countByReceipt(ids));
        Map<UUID, Integer> pending = toCountMap(receiptLines.countByReceiptAndPutawayStatus(ids, PutawayStatus.PENDING));

        Set<UUID> poIds = content.stream().map(r -> r.getPurchaseOrder().getId()).collect(Collectors.toSet());
        Map<UUID, String> poNumbers = purchaseOrders.findAllById(poIds).stream()
                .collect(Collectors.toMap(PurchaseOrder::getId, PurchaseOrder::getPoNumber));
        Map<UUID, String> userNames = workers.findDisplayNames(
                content.stream().map(GoodsReceipt::getReceivedBy).collect(Collectors.toSet()));

        return r -> GoodsReceiptMapper.toSummary(r,
                poNumbers.get(r.getPurchaseOrder().getId()),
                userNames.get(r.getReceivedBy()),
                lineCounts.getOrDefault(r.getId(), 0),
                pending.getOrDefault(r.getId(), 0));
    }

    private static Map<UUID, Integer> toCountMap(List<GoodsReceiptLineRepository.ReceiptCount> rows) {
        return rows.stream().collect(Collectors.toMap(
                GoodsReceiptLineRepository.ReceiptCount::getReceiptId,
                row -> Math.toIntExact(row.getLineCount())));
    }
}
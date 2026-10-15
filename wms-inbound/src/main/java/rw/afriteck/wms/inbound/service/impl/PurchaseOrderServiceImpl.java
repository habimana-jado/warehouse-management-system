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
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.BarcodeResolution;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.SupplierService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.mapper.PurchaseOrderMapper;
import rw.afriteck.wms.inbound.repository.PurchaseOrderLineRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderSpecs;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;
import rw.afriteck.wms.inbound.support.InboundDefaults;
import rw.afriteck.wms.inbound.support.InboundNumbers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("poNumber", "orderDate", "expectedDate", "status", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "orderDate");
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PurchaseOrderRepository purchaseOrders;
    private final PurchaseOrderLineRepository orderLines;
    private final SupplierService suppliers;
    private final WarehouseService warehouses;
    private final ItemService items;
    private final WorkerLookup workers;
    private final Clock clock;

    // ------------------------------------------------------------------ commands

    @Override
    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest r) {
        if (r == null || r.supplierId() == null || r.warehouseId() == null) {
            throw new BusinessException("PO_FIELDS_REQUIRED", "Supplier and warehouse are required");
        }
        requireActiveSupplier(r.supplierId());
        requireActiveWarehouse(r.warehouseId());

        LocalDate orderDate = r.orderDate() != null ? r.orderDate() : LocalDate.now(clock);
        checkDates(orderDate, r.expectedDate());

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(InboundNumbers.purchaseOrder(purchaseOrders.nextPoNumber()));
        po.setSupplierId(r.supplierId());
        po.setWarehouseId(r.warehouseId());
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setOrderDate(orderDate);
        po.setExpectedDate(r.expectedDate());
        po.setSupplierReference(Codes.blankToNull(r.supplierReference()));
        po.setCurrency(normalizeCurrency(r.currency()));
        po.setNotes(Codes.blankToNull(r.notes()));

        List<PurchaseOrderLineRequest> requested = r.lines() == null ? List.of() : r.lines();
        Map<UUID, ItemResponse> itemMap = loadActiveItems(itemIds(requested));
        int lineNo = 0;
        for (PurchaseOrderLineRequest req : requested) {
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setLineNo(++lineNo);
            applyLine(line, req, itemMap.get(req.itemId()));
            po.addLine(line);
        }
        return toResponse(purchaseOrders.saveAndFlush(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse update(UUID id, UpdatePurchaseOrderRequest r) {
        PurchaseOrder po = lockForUpdate(id);
        requireStatus(po, "Only a DRAFT purchase order can be edited", PurchaseOrderStatus.DRAFT);

        checkDates(po.getOrderDate(), r.expectedDate());
        po.setExpectedDate(r.expectedDate());
        po.setSupplierReference(Codes.blankToNull(r.supplierReference()));
        po.setNotes(Codes.blankToNull(r.notes()));
        if (Codes.blankToNull(r.currency()) != null) {
            po.setCurrency(normalizeCurrency(r.currency()));       // blank = keep the current currency
        }
        if (r.lines() != null) {
            syncLines(po, r.lines());
        }
        return toResponse(purchaseOrders.saveAndFlush(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse confirm(UUID id, UUID actorId) {
        PurchaseOrder po = lockForUpdate(id);
        requireStatus(po, "Only a DRAFT purchase order can be confirmed", PurchaseOrderStatus.DRAFT);
        if (po.getLines().isEmpty()) {
            throw new BusinessException("PO_NO_LINES", "A purchase order needs at least one line");
        }
        // things can change between draft and confirmation: re-check them
        requireActiveSupplier(po.getSupplierId());
        requireActiveWarehouse(po.getWarehouseId());
        loadActiveItems(po.getLines().stream().map(PurchaseOrderLine::getItemId).collect(Collectors.toSet()));

        po.setStatus(PurchaseOrderStatus.CONFIRMED);
        po.setConfirmedAt(clock.instant());
        po.setConfirmedBy(actorId);
        return toResponse(purchaseOrders.saveAndFlush(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancel(UUID id, String reason) {
        String why = Codes.blankToNull(reason);
        if (why == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancellation reason is required");
        }
        PurchaseOrder po = lockForUpdate(id);
        if (po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ConflictException("PO_HAS_RECEIPTS",
                    "Goods were already received on this purchase order; close it instead");
        }
        requireStatus(po, "Only a DRAFT or CONFIRMED purchase order can be cancelled",
                PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.CONFIRMED);

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po.setCancelledAt(clock.instant());
        po.setCancelReason(why);
        return toResponse(purchaseOrders.saveAndFlush(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse close(UUID id, UUID actorId) {
        PurchaseOrder po = lockForUpdate(id);
        if (po.getStatus() == PurchaseOrderStatus.CONFIRMED) {
            throw new ConflictException("PO_INVALID_STATE", "Nothing was received yet; cancel the purchase order instead");
        }
        requireStatus(po, "Only a PARTIALLY_RECEIVED purchase order can be closed", PurchaseOrderStatus.PARTIALLY_RECEIVED);

        po.setStatus(PurchaseOrderStatus.CLOSED);
        po.setClosedAt(clock.instant());
        po.setClosedBy(actorId);
        return toResponse(purchaseOrders.saveAndFlush(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse changeTolerance(UUID id, UUID lineId, BigDecimal tolerancePercent) {
        BigDecimal tolerance = checkTolerance(tolerancePercent);
        PurchaseOrder po = lockForUpdate(id);
        requireStatus(po, "Tolerance can only be changed on an open purchase order (draft lines are edited with the order)",
                PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

        PurchaseOrderLine line = po.getLines().stream()
                .filter(l -> l.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("PurchaseOrderLine", lineId));
        line.setTolerancePercent(tolerance);
        return toResponse(purchaseOrders.saveAndFlush(po));
    }

    // ------------------------------------------------------------------ queries

    @Override
    public PurchaseOrderResponse getById(UUID id) {
        PurchaseOrder po = purchaseOrders.findWithLinesById(id)
                .orElseThrow(() -> new NotFoundException("PurchaseOrder", id));
        return toResponse(po);
    }

    @Override
    public PageResponse<PurchaseOrderSummaryResponse> search(PurchaseOrderFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
        if (f.orderDateFrom() != null && f.orderDateTo() != null && f.orderDateFrom().isAfter(f.orderDateTo())) {
            throw new BusinessException("INVALID_DATE_RANGE", "The start date is after the end date");
        }
        Pageable safe = PageRules.sanitize(pageable, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<PurchaseOrder> page = purchaseOrders.findAll(PurchaseOrderSpecs.of(f), safe);

        List<UUID> ids = page.getContent().stream().map(PurchaseOrder::getId).toList();
        Map<UUID, PurchaseOrderLineRepository.Totals> totals = ids.isEmpty()
                ? Map.of()
                : orderLines.totalsByOrder(ids).stream()
                .collect(Collectors.toMap(PurchaseOrderLineRepository.Totals::getPurchaseOrderId, Function.identity()));
        Map<UUID, String> supplierNames = supplierNames(page.getContent());

        return PageResponse.of(page.map(po -> {
            PurchaseOrderLineRepository.Totals t = totals.get(po.getId());
            return PurchaseOrderMapper.toSummary(po, supplierNames.get(po.getSupplierId()),
                    t == null ? 0 : Math.toIntExact(t.getLineCount()),
                    t == null ? null : t.getOrderedQty(),
                    t == null ? null : t.getAcceptedQty());
        }));
    }

    @Override
    public PurchaseOrderLineScanResponse scan(UUID id, String barcode) {
        String code = Codes.blankToNull(barcode);
        if (code == null) {
            throw new BusinessException("BARCODE_REQUIRED", "A barcode is required");
        }
        PurchaseOrder po = purchaseOrders.findWithLinesById(id)
                .orElseThrow(() -> new NotFoundException("PurchaseOrder", id));

        BarcodeResolution resolution = items.resolveBarcode(code);
        UUID itemId = resolution.item().id();

        // the same item can sit on two lines: prefer a line that still needs goods, then the lowest line number
        PurchaseOrderLine line = po.getLines().stream()
                .filter(l -> l.getItemId().equals(itemId))
                .min(Comparator
                        .comparingInt((PurchaseOrderLine l) -> l.remainingQty().signum() > 0 ? 0 : 1)
                        .thenComparingInt(PurchaseOrderLine::getLineNo))
                .orElseThrow(() -> new BusinessException("ITEM_NOT_ON_PURCHASE_ORDER",
                        "This item is not on the purchase order"));

        return new PurchaseOrderLineScanResponse(
                PurchaseOrderMapper.toLineResponse(line, Map.of(itemId, resolution.item())),
                resolution.uom(),
                resolution.factorToBase());
    }

    @Override
    public UUID warehouseIdOf(UUID id) {
        return purchaseOrders.findById(id)
                .map(PurchaseOrder::getWarehouseId)
                .orElseThrow(() -> new NotFoundException("PurchaseOrder", id));
    }

    // ------------------------------------------------------------------ line handling

    /** Diff-based sync: never clear and re-add (Hibernate runs inserts before deletes). */
    private void syncLines(PurchaseOrder po, List<PurchaseOrderLineRequest> requested) {
        Set<UUID> seen = new HashSet<>();
        for (PurchaseOrderLineRequest req : requested) {
            if (req.id() != null && !seen.add(req.id())) {
                throw new BusinessException("PO_LINE_DUPLICATED", "The same purchase order line appears twice");
            }
        }
        Map<UUID, PurchaseOrderLine> existing = po.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, Function.identity()));
        Map<UUID, ItemResponse> itemMap = loadActiveItems(itemIds(requested));

        int lastLineNo = po.getLines().stream().mapToInt(PurchaseOrderLine::getLineNo).max().orElse(0);
        Set<UUID> kept = new HashSet<>();

        for (PurchaseOrderLineRequest req : requested) {
            ItemResponse item = itemMap.get(req.itemId());
            if (req.id() == null) {
                PurchaseOrderLine line = new PurchaseOrderLine();
                line.setLineNo(++lastLineNo);                  // never reuse a number: no unique-key clash on flush
                applyLine(line, req, item);
                po.addLine(line);
            } else {
                PurchaseOrderLine line = existing.get(req.id());
                if (line == null) {
                    throw new BusinessException("PO_LINE_NOT_FOUND", "Line " + req.id() + " is not on this purchase order");
                }
                applyLine(line, req, item);
                kept.add(line.getId());
            }
        }
        po.getLines().removeIf(l -> l.getId() != null && existing.containsKey(l.getId()) && !kept.contains(l.getId()));
    }

    /** Validates one requested line, converts box quantities and prices to the base UOM and copies the values. */
    private void applyLine(PurchaseOrderLine line, PurchaseOrderLineRequest req, ItemResponse item) {
        if (req.quantity() == null || req.quantity().signum() <= 0) {
            throw new BusinessException("QTY_INVALID", "Quantity must be greater than zero (item " + item.sku() + ")");
        }
        if (req.unitPrice() != null && req.unitPrice().signum() < 0) {
            throw new BusinessException("PRICE_INVALID", "Unit price cannot be negative (item " + item.sku() + ")");
        }

        String uom = Codes.blankToNull(req.uom());
        BigDecimal baseQty;
        BigDecimal basePrice = req.unitPrice();
        if (uom == null || uom.equalsIgnoreCase(item.baseUom())) {
            baseQty = req.quantity().setScale(4, RoundingMode.HALF_UP);
        } else {
            baseQty = items.toBaseQuantity(item.id(), Codes.normalize(uom), req.quantity())
                    .setScale(4, RoundingMode.HALF_UP);
            if (basePrice != null && baseQty.signum() > 0) {
                // price per base unit that keeps the line total unchanged
                basePrice = req.unitPrice().multiply(req.quantity()).divide(baseQty, 4, RoundingMode.HALF_UP);
            }
        }
        if (baseQty.signum() <= 0) {
            throw new BusinessException("QTY_INVALID", "Quantity is too small in the base unit (item " + item.sku() + ")");
        }

        line.setItemId(item.id());
        line.setOrderedQty(baseQty);
        line.setUnitPrice(basePrice == null ? null : basePrice.setScale(4, RoundingMode.HALF_UP));
        line.setTolerancePercent(checkTolerance(req.tolerancePercent()));
        line.setNotes(Codes.blankToNull(req.notes()));
    }

    private static Set<UUID> itemIds(List<PurchaseOrderLineRequest> requested) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (PurchaseOrderLineRequest req : requested) {
            if (req.itemId() == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Every line needs an item");
            }
            ids.add(req.itemId());
        }
        return ids;
    }

    /** One batch lookup. Every id must exist and be active (ITEM_NOT_FOUND / ITEM_INACTIVE). */
    private Map<UUID, ItemResponse> loadActiveItems(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ItemResponse> found = items.findAllByIds(ids).stream()
                .collect(Collectors.toMap(ItemResponse::id, Function.identity()));
        for (UUID id : ids) {
            ItemResponse item = found.get(id);
            if (item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item " + id + " does not exist");
            }
            if (!item.active()) {
                throw new BusinessException("ITEM_INACTIVE", "Item " + item.sku() + " is inactive");
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ validation helpers

    private PurchaseOrder lockForUpdate(UUID id) {
        return purchaseOrders.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("PurchaseOrder", id));
    }

    private static void requireStatus(PurchaseOrder po, String message, PurchaseOrderStatus... allowed) {
        for (PurchaseOrderStatus s : allowed) {
            if (po.getStatus() == s) {
                return;
            }
        }
        throw new ConflictException("PO_INVALID_STATE", message + " (current status: " + po.getStatus() + ")");
    }

    private void requireActiveSupplier(UUID supplierId) {
        SupplierResponse supplier = suppliers.getById(supplierId);          // NotFoundException if unknown
        if (!supplier.active()) {
            throw new BusinessException("SUPPLIER_INACTIVE", "Supplier " + supplier.name() + " is inactive");
        }
    }

    private void requireActiveWarehouse(UUID warehouseId) {
        WarehouseResponse warehouse = warehouses.getById(warehouseId);      // NotFoundException if unknown
        if (!warehouse.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse " + warehouse.name() + " is inactive");
        }
    }

    private static void checkDates(LocalDate orderDate, LocalDate expectedDate) {
        if (expectedDate != null && expectedDate.isBefore(orderDate)) {
            throw new BusinessException("PO_DATES_INVALID", "The expected date is before the order date");
        }
    }

    private static String normalizeCurrency(String raw) {
        String c = Codes.blankToNull(raw);
        if (c == null) {
            return InboundDefaults.DEFAULT_CURRENCY;
        }
        c = Codes.normalize(c);
        if (!CURRENCY.matcher(c).matches()) {
            throw new BusinessException("CURRENCY_INVALID", "Currency must be a 3-letter code such as RWF or USD");
        }
        return c;
    }

    private static BigDecimal checkTolerance(BigDecimal raw) {
        BigDecimal t = raw == null ? BigDecimal.ZERO : raw;
        if (t.signum() < 0 || t.compareTo(HUNDRED) > 0) {
            throw new BusinessException("TOLERANCE_INVALID", "Tolerance must be between 0 and 100 percent");
        }
        return t.setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ response assembly (batch lookups, then the static mapper)

    private PurchaseOrderResponse toResponse(PurchaseOrder po) {
        String supplierName = suppliers.getById(po.getSupplierId()).name();
        Set<UUID> itemIds = po.getLines().stream().map(PurchaseOrderLine::getItemId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> itemMap = itemIds.isEmpty()
                ? Map.of()
                : items.findAllByIds(itemIds).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));

        Set<UUID> userIds = new HashSet<>();
        if (po.getConfirmedBy() != null) userIds.add(po.getConfirmedBy());
        if (po.getClosedBy() != null) userIds.add(po.getClosedBy());
        Map<UUID, String> userNames = userIds.isEmpty() ? Map.of() : workers.findDisplayNames(userIds);

        return PurchaseOrderMapper.toResponse(po, supplierName, itemMap, userNames);
    }

    /** SupplierService has no batch method yet: one call per DISTINCT supplier on the page (usually a handful). */
    private Map<UUID, String> supplierNames(List<PurchaseOrder> pos) {
        Map<UUID, String> names = new HashMap<>();
        for (UUID id : pos.stream().map(PurchaseOrder::getSupplierId).collect(Collectors.toSet())) {
            names.put(id, suppliers.getById(id).name());
        }
        return names;
    }
}
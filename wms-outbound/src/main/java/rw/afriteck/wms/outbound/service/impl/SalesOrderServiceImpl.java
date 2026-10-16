package rw.afriteck.wms.outbound.service.impl;

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
import rw.afriteck.wms.common.support.Chunks;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.mappers.SalesOrderMapper;
import rw.afriteck.wms.outbound.repository.OrderTotals;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.repository.SalesOrderSpecs;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.outbound.support.OutboundDefaults;
import rw.afriteck.wms.outbound.support.OutboundNumbers;

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
@Transactional
public class SalesOrderServiceImpl implements SalesOrderService {

    private static final Set<String> SORTS = Set.of("orderNumber", "orderDate", "requiredDate", "status", "createdAt");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    private static final int PRICE_SCALE = 4;

    private final SalesOrderRepository orders;
    private final PickListService pickLists;
    private final CustomerService customers;
    private final WarehouseService warehouses;
    private final ItemService items;
    private final InventoryQueryService inventoryQuery;
    private final OutboundNumbers numbers;
    private final Clock clock;

    private record PreparedLine(UUID itemId, BigDecimal baseQty, BigDecimal basePrice, String note) {
    }

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public SalesOrderResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID warehouseIdOf(UUID id) {
        return load(id).getWarehouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SalesOrderSummaryResponse> search(SalesOrderFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
        if (f.orderDateFrom() != null && f.orderDateTo() != null && f.orderDateFrom().isAfter(f.orderDateTo())) {
            throw new BusinessException("INVALID_DATE_RANGE", "The start date is after the end date");
        }
        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SalesOrder> page = orders.findAll(SalesOrderSpecs.from(f), safe);

        List<UUID> ids = page.getContent().stream().map(SalesOrder::getId).toList();
        Map<UUID, OrderTotals> totals = new HashMap<>();
        if (!ids.isEmpty()) {
            for (List<UUID> chunk : Chunks.partition(ids)) {
                orders.totalsByOrder(chunk).forEach(t -> totals.put(t.getOrderId(), t));
            }
        }
        Map<UUID, String> names = new HashMap<>();
        page.getContent().stream().map(SalesOrder::getCustomerId).distinct()
                .forEach(cid -> names.put(cid, customerName(cid)));

        return PageResponse.of(page.map(o ->
                SalesOrderMapper.toSummary(o, names.get(o.getCustomerId()), totals.get(o.getId()))));
    }

    // ----------------------------------------------------------------- create

    @Override
    public SalesOrderResponse create(CreateSalesOrderRequest r) {
        if (r == null || r.customerId() == null || r.warehouseId() == null) {
            throw new BusinessException("SO_FIELDS_REQUIRED", "Customer and warehouse are required");
        }
        CustomerResponse customer = customers.getById(r.customerId());
        if (!customer.active()) {
            throw new BusinessException("CUSTOMER_INACTIVE", "The customer is inactive");
        }
        WarehouseResponse warehouse = warehouses.getById(r.warehouseId());
        if (!warehouse.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "The warehouse is inactive");
        }
        LocalDate orderDate = r.orderDate() != null ? r.orderDate() : LocalDate.now(clock);
        checkDates(orderDate, r.requiredDate());
        checkLength(r.customerReference(), 60, "Customer reference");
        checkLength(r.notes(), 1000, "Notes");
        List<PreparedLine> prepared = prepareLines(r.lines());

        SalesOrder o = new SalesOrder();
        o.setOrderNumber(numbers.nextSalesOrder());
        o.setCustomerId(r.customerId());
        o.setWarehouseId(r.warehouseId());
        o.setStatus(SalesOrderStatus.DRAFT);
        o.setOrderDate(orderDate);
        o.setRequiredDate(r.requiredDate());
        o.setCurrency(normalizeCurrency(r.currency()));
        o.setCustomerReference(Codes.blankToNull(r.customerReference()));
        o.setNotes(Codes.blankToNull(r.notes()));
        int lineNo = 1;
        for (PreparedLine p : prepared) {
            SalesOrderLine l = new SalesOrderLine();
            l.setLineNo(lineNo++);
            apply(l, p);
            o.addLine(l);
        }
        o = orders.saveAndFlush(o);
        return SalesOrderMapper.toResponse(o, customer.name(), itemsById(itemIds(o)));
    }

    // ----------------------------------------------------------------- update

    /**
     * PUT semantics: requiredDate, customerReference and notes are replaced (null clears); null orderDate or currency keeps the old value.
     */
    @Override
    public SalesOrderResponse update(UUID id, UpdateSalesOrderRequest r) {
        if (r == null) {
            throw new BusinessException("SO_FIELDS_REQUIRED", "The request is empty");
        }
        SalesOrder o = loadForUpdate(id);
        requireStatus(o, EnumSet.of(SalesOrderStatus.DRAFT));

        LocalDate orderDate = r.orderDate() != null ? r.orderDate() : o.getOrderDate();
        checkDates(orderDate, r.requiredDate());
        checkLength(r.customerReference(), 60, "Customer reference");
        checkLength(r.notes(), 1000, "Notes");
        List<PreparedLine> prepared = prepareLines(r.lines());

        o.setOrderDate(orderDate);
        o.setRequiredDate(r.requiredDate());
        if (r.currency() != null && !r.currency().isBlank()) {
            o.setCurrency(normalizeCurrency(r.currency()));
        }
        o.setCustomerReference(Codes.blankToNull(r.customerReference()));
        o.setNotes(Codes.blankToNull(r.notes()));
        syncLines(o, prepared);
        return toResponse(orders.saveAndFlush(o));
    }

    /**
     * Diff sync matched on itemId (one line per item): update in place, add with max line number + 1, remove the rest.
     */
    private void syncLines(SalesOrder o, List<PreparedLine> prepared) {
        Map<UUID, SalesOrderLine> existing = new HashMap<>();
        o.getLines().forEach(l -> existing.put(l.getItemId(), l));
        int next = o.getLines().stream().mapToInt(SalesOrderLine::getLineNo).max().orElse(0) + 1;

        Set<UUID> keep = new HashSet<>();
        for (PreparedLine p : prepared) {
            SalesOrderLine l = existing.get(p.itemId());
            if (l == null) {
                l = new SalesOrderLine();
                l.setLineNo(next++);
                apply(l, p);
                o.addLine(l);
            } else {
                apply(l, p);
            }
            keep.add(p.itemId());
        }
        o.getLines().removeIf(l -> !keep.contains(l.getItemId()));
    }

    // ---------------------------------------------------------------- confirm

    @Override
    public ConfirmSalesOrderResponse confirm(UUID id) {
        SalesOrder o = loadForUpdate(id);
        requireStatus(o, EnumSet.of(SalesOrderStatus.DRAFT));

        CustomerResponse customer = customers.getById(o.getCustomerId());
        if (!customer.active()) {
            throw new BusinessException("CUSTOMER_INACTIVE", "The customer is inactive");
        }
        if (!warehouses.getById(o.getWarehouseId()).active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "The warehouse is inactive");
        }
        Map<UUID, ItemResponse> itemMap = itemsById(itemIds(o));
        for (SalesOrderLine l : o.getLines()) {
            ItemResponse item = itemMap.get(l.getItemId());
            if (item == null) throw new BusinessException("ITEM_NOT_FOUND", "Item not found on line " + l.getLineNo());
            if (!item.active()) throw new BusinessException("ITEM_INACTIVE", "Item " + item.sku() + " is inactive");
        }

        o.setStatus(SalesOrderStatus.CONFIRMED);
        o.setConfirmedAt(clock.instant());
        o = orders.saveAndFlush(o);

        return new ConfirmSalesOrderResponse(
                SalesOrderMapper.toResponse(o, customer.name(), itemMap),
                availabilityWarnings(o, itemMap));
    }

    /**
     * Advisory only: STORAGE bins, this order against the warehouse. Other open orders are not netted (no reservations).
     */
    private List<AvailabilityWarning> availabilityWarnings(SalesOrder o, Map<UUID, ItemResponse> itemMap) {
        Map<UUID, BigDecimal> available = inventoryQuery.getAvailableInStorage(o.getWarehouseId(), itemIds(o));
        List<AvailabilityWarning> warnings = new ArrayList<>();
        for (SalesOrderLine l : o.getLines()) {
            BigDecimal have = available.getOrDefault(l.getItemId(), BigDecimal.ZERO);
            if (have.compareTo(l.getOrderedQty()) < 0) {
                ItemResponse item = itemMap.get(l.getItemId());
                warnings.add(new AvailabilityWarning(l.getItemId(),
                        item == null ? null : item.sku(), item == null ? null : item.name(),
                        l.getOrderedQty(), have));
            }
        }
        return warnings;
    }

    // ----------------------------------------------------------------- cancel

    @Override
    public SalesOrderResponse cancel(UUID id, CancelSalesOrderRequest r) {
        String reason = r == null ? null : Codes.blankToNull(r.reason());
        if (reason == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancel reason is required");
        }
        checkLength(reason, 500, "Reason");
        SalesOrder o = loadForUpdate(id);
        requireStatus(o, EnumSet.of(SalesOrderStatus.DRAFT, SalesOrderStatus.CONFIRMED));
        if (o.hasPickedStock()) {
            throw new ConflictException("SO_HAS_PICKS",
                    "Stock was already picked for this order: ship it or close the order short");
        }
        pickLists.cancelOpenForOrder(o.getId(), reason);       // cancels the PICK tasks, nothing was picked
        o.setStatus(SalesOrderStatus.CANCELLED);
        o.setCancelReason(reason);
        return toResponse(orders.saveAndFlush(o));
    }

    // ------------------------------------------------------------------ close

    /**
     * Close short: the unshipped remainder is abandoned. Only when everything picked has been shipped.
     */
    @Override
    public SalesOrderResponse close(UUID id) {
        SalesOrder o = loadForUpdate(id);
        requireStatus(o, EnumSet.of(SalesOrderStatus.PARTIALLY_SHIPPED));
        boolean pickedNotShipped = o.getLines().stream().anyMatch(l -> l.readyToShipQty().signum() > 0);
        if (pickedNotShipped) {
            throw new ConflictException("SO_HAS_UNSHIPPED_PICKS", "Ship the picked goods before closing the order");
        }
        pickLists.cancelOpenForOrder(o.getId(), "Order closed short");
        o.setStatus(SalesOrderStatus.CLOSED);
        o.setClosedAt(clock.instant());
        return toResponse(orders.saveAndFlush(o));
    }

    // ---------------------------------------------------------------- helpers

    private SalesOrder load(UUID id) {
        return orders.findById(id).orElseThrow(() -> new NotFoundException("SalesOrder", id));
    }

    /**
     * Locks the header only; lines are read lazily afterwards (never FOR UPDATE with a collection fetch).
     */
    private SalesOrder loadForUpdate(UUID id) {
        return orders.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("SalesOrder", id));
    }

    private void requireStatus(SalesOrder o, Set<SalesOrderStatus> allowed) {
        if (!allowed.contains(o.getStatus())) {
            throw new ConflictException("SO_INVALID_STATE", "The order is " + o.getStatus() + ": action not allowed");
        }
    }

    private void apply(SalesOrderLine l, PreparedLine p) {
        l.setItemId(p.itemId());
        l.setOrderedQty(p.baseQty());
        l.setUnitPrice(p.basePrice());
        l.setNote(p.note());
    }

    /**
     * Validates every line and converts quantity and price to the item BASE UOM (the line total is preserved).
     */
    private List<PreparedLine> prepareLines(List<SalesOrderLineRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new BusinessException("SO_NO_LINES", "An order needs at least one line");
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (SalesOrderLineRequest r : requested) {
            if (r == null || r.itemId() == null) {
                throw new BusinessException("SO_FIELDS_REQUIRED", "Every line needs an item");
            }
            if (!ids.add(r.itemId())) {
                throw new BusinessException("SO_LINE_DUPLICATED", "The same item appears on more than one line");
            }
        }
        Map<UUID, ItemResponse> itemMap = itemsById(ids);

        List<PreparedLine> out = new ArrayList<>();
        for (SalesOrderLineRequest r : requested) {
            ItemResponse item = itemMap.get(r.itemId());
            if (item == null) throw new BusinessException("ITEM_NOT_FOUND", "Item not found: " + r.itemId());
            if (!item.active()) throw new BusinessException("ITEM_INACTIVE", "Item " + item.sku() + " is inactive");
            if (r.quantity() == null || r.quantity().signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Quantity must be greater than 0 for " + item.sku());
            }
            if (r.unitPrice() != null && r.unitPrice().signum() < 0) {
                throw new BusinessException("PRICE_INVALID", "Price cannot be negative for " + item.sku());
            }
            checkLength(r.note(), 255, "Line note");

            String uom = Codes.blankToNull(r.uom());
            BigDecimal baseQty = uom == null ? r.quantity()
                    : items.toBaseQuantity(r.itemId(), Codes.normalize(uom), r.quantity());
            if (baseQty.signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Quantity must be greater than 0 for " + item.sku());
            }
            BigDecimal basePrice = r.unitPrice() == null ? null
                    : r.unitPrice().multiply(r.quantity()).divide(baseQty, PRICE_SCALE, RoundingMode.HALF_UP);
            out.add(new PreparedLine(r.itemId(), baseQty, basePrice, Codes.blankToNull(r.note())));
        }
        return out;
    }

    private String normalizeCurrency(String raw) {
        String c = Codes.blankToNull(raw);
        if (c == null) return OutboundDefaults.DEFAULT_CURRENCY;
        c = Codes.normalize(c);
        if (!CURRENCY.matcher(c).matches()) {
            throw new BusinessException("CURRENCY_INVALID", "Currency must be 3 capital letters");
        }
        return c;
    }

    private void checkDates(LocalDate orderDate, LocalDate requiredDate) {
        if (requiredDate != null && requiredDate.isBefore(orderDate)) {
            throw new BusinessException("SO_DATES_INVALID", "The required date is before the order date");
        }
    }

    private void checkLength(String text, int max, String field) {
        if (text != null && text.length() > max) {
            throw new BusinessException("TEXT_TOO_LONG", field + " is longer than " + max + " characters");
        }
    }

    private Set<UUID> itemIds(SalesOrder o) {
        return o.getLines().stream().map(SalesOrderLine::getItemId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<UUID, ItemResponse> itemsById(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return items.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    private String customerName(UUID customerId) {
        return customers.getById(customerId).name();
    }

    private SalesOrderResponse toResponse(SalesOrder o) {
        return SalesOrderMapper.toResponse(o, customerName(o.getCustomerId()), itemsById(itemIds(o)));
    }
}

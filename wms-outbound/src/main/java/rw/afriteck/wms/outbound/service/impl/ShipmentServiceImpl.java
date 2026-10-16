package rw.afriteck.wms.outbound.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;
import rw.afriteck.wms.outbound.entity.Shipment;
import rw.afriteck.wms.outbound.entity.ShipmentLine;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.enums.ShipmentStatus;
import rw.afriteck.wms.outbound.mappers.ShipmentMapper;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.repository.ShipmentRepository;
import rw.afriteck.wms.outbound.service.ShipmentService;
import rw.afriteck.wms.outbound.support.OutboundNumbers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentServiceImpl implements ShipmentService {

    private static final Set<String> SORTS = Set.of("shippedAt", "shipmentNumber", "status", "createdAt");

    private final SalesOrderRepository orders;
    private final ShipmentRepository shipments;
    private final CustomerService customers;
    private final ItemService items;
    private final OutboundNumbers numbers;
    private final Clock clock;

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID warehouseIdOf(UUID shipmentId) {
        return load(shipmentId).getWarehouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentSummaryResponse> findBySalesOrder(UUID salesOrderId) {
        return toSummaries(shipments.findBySalesOrderIdOrderByShippedAtDesc(salesOrderId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShipmentSummaryResponse> search(UUID warehouseId, String q, Pageable pageable) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
        String pattern = q == null || q.isBlank() ? "%" : Codes.likePattern(q);     // never a null into lower(...)
        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "shippedAt"));
        Page<Shipment> page = shipments.search(warehouseId, pattern, safe);
        List<ShipmentSummaryResponse> content = toSummaries(page.getContent());
        return PageResponse.of(new PageImpl<>(content, safe, page.getTotalElements()));
    }

    // ----------------------------------------------------------------- create

    /**
     * Dispatches what is picked and not yet shipped. No stock is posted here: the stock left the ledger at the pick.
     */
    @Override
    public ShipmentResponse create(UUID salesOrderId, CreateShipmentRequest r) {
        SalesOrder o = orders.findByIdForUpdate(salesOrderId)
                .orElseThrow(() -> new NotFoundException("SalesOrder", salesOrderId));
        if (!SalesOrderStatus.RELEASABLE.contains(o.getStatus())) {
            throw new ConflictException("SO_INVALID_STATE", "The order is " + o.getStatus() + ": nothing can be shipped");
        }
        String address = r == null ? null : Codes.blankToNull(r.shipToAddress());
        String carrier = r == null ? null : Codes.blankToNull(r.carrier());
        String tracking = r == null ? null : Codes.blankToNull(r.trackingReference());
        String notes = r == null ? null : Codes.blankToNull(r.notes());
        Integer packages = r == null ? null : r.packageCount();
        checkLength(address, 500, "Ship-to address");
        checkLength(carrier, 100, "Carrier");
        checkLength(tracking, 100, "Tracking reference");
        checkLength(notes, 1000, "Notes");
        if (packages != null && packages <= 0) {
            throw new BusinessException("PACKAGE_COUNT_INVALID", "The package count must be greater than 0");
        }

        Map<SalesOrderLine, BigDecimal> toShip = resolveQuantities(o, r == null ? null : r.lines());

        Shipment s = new Shipment();
        s.setShipmentNumber(numbers.nextShipment());
        s.setSalesOrder(o);
        s.setCustomerId(o.getCustomerId());
        s.setWarehouseId(o.getWarehouseId());
        s.setStatus(ShipmentStatus.DISPATCHED);
        s.setShippedAt(clock.instant());
        s.setShipToAddress(address != null ? address : customerAddress(o.getCustomerId()));
        s.setCarrier(carrier);
        s.setTrackingReference(tracking);
        s.setPackageCount(packages);
        s.setNotes(notes);

        int lineNo = 1;
        for (SalesOrderLine sol : o.getLines()) {                    // order lines keep their own order on the delivery note
            BigDecimal qty = toShip.get(sol);
            if (qty == null) continue;
            ShipmentLine sl = new ShipmentLine();
            sl.setLineNo(lineNo++);
            sl.setSalesOrderLine(sol);
            sl.setItemId(sol.getItemId());
            sl.setQuantity(qty);
            s.addLine(sl);
            sol.setShippedQty(sol.getShippedQty().add(qty));
        }

        boolean allFulfilled = o.getLines().stream().allMatch(SalesOrderLine::isFulfilled);
        if (allFulfilled) {
            o.setStatus(SalesOrderStatus.SHIPPED);
            o.setClosedAt(s.getShippedAt());
        } else {
            o.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);
        }
        return toResponse(shipments.saveAndFlush(s));
    }

    /**
     * null or empty request lines = everything ready to ship; otherwise validated against what is picked and unshipped.
     */
    private Map<SalesOrderLine, BigDecimal> resolveQuantities(SalesOrder o, List<ShipmentLineRequest> requested) {
        Map<SalesOrderLine, BigDecimal> result = new LinkedHashMap<>();
        if (requested == null || requested.isEmpty()) {
            for (SalesOrderLine l : o.getLines()) {
                if (l.readyToShipQty().signum() > 0) result.put(l, l.readyToShipQty());
            }
        } else {
            Map<UUID, SalesOrderLine> byId = new HashMap<>();
            o.getLines().forEach(l -> byId.put(l.getId(), l));
            for (ShipmentLineRequest lr : requested) {
                SalesOrderLine l = lr == null || lr.salesOrderLineId() == null ? null : byId.get(lr.salesOrderLineId());
                if (l == null) {
                    throw new BusinessException("SHIPMENT_LINE_NOT_FOUND", "The line does not belong to this order");
                }
                if (result.containsKey(l)) {
                    throw new BusinessException("SHIPMENT_LINE_DUPLICATED", "The same order line appears twice");
                }
                if (lr.quantity() == null || lr.quantity().signum() <= 0) {
                    throw new BusinessException("QTY_INVALID", "Quantity must be greater than 0 on line " + l.getLineNo());
                }
                if (lr.quantity().compareTo(l.readyToShipQty()) > 0) {
                    throw new BusinessException("SHIP_QTY_EXCEEDS_PICKED",
                            "Line " + l.getLineNo() + ": only " + l.readyToShipQty() + " is picked and not yet shipped");
                }
                result.put(l, lr.quantity());
            }
        }
        if (result.isEmpty()) {
            throw new BusinessException("SHIPMENT_NOTHING_TO_SHIP", "Nothing is picked and waiting to be shipped");
        }
        return result;
    }

    // -------------------------------------------------------- confirm delivery

    /**
     * Proof of delivery is optional and does not change the order status.
     */
    @Override
    public ShipmentResponse confirmDelivery(UUID shipmentId, ConfirmDeliveryRequest r) {
        Shipment s = load(shipmentId);
        if (s.getStatus() != ShipmentStatus.DISPATCHED) {
            throw new ConflictException("SHIPMENT_INVALID_STATE", "The shipment is " + s.getStatus());
        }
        Instant now = clock.instant();
        Instant deliveredAt = r == null || r.deliveredAt() == null ? now : r.deliveredAt();
        if (deliveredAt.isBefore(s.getShippedAt()) || deliveredAt.isAfter(now)) {
            throw new BusinessException("DELIVERY_DATE_INVALID", "The delivery time must be between dispatch and now");
        }
        String receivedBy = r == null ? null : Codes.blankToNull(r.receivedByName());
        String podNotes = r == null ? null : Codes.blankToNull(r.podNotes());
        checkLength(receivedBy, 120, "Received by");
        checkLength(podNotes, 500, "Notes");

        s.setStatus(ShipmentStatus.DELIVERED);
        s.setDeliveredAt(deliveredAt);
        s.setReceivedByName(receivedBy);
        s.setPodNotes(podNotes);
        return toResponse(shipments.saveAndFlush(s));
    }

    // ---------------------------------------------------------------- helpers

    private Shipment load(UUID id) {
        return shipments.findById(id).orElseThrow(() -> new NotFoundException("Shipment", id));
    }

    private void checkLength(String text, int max, String field) {
        if (text != null && text.length() > max) {
            throw new BusinessException("TEXT_TOO_LONG", field + " is longer than " + max + " characters");
        }
    }

    /**
     * The customer's address as a snapshot on the delivery note (editable at dispatch).
     */
    private String customerAddress(UUID customerId) {
        CustomerResponse c = customers.getById(customerId);
        String joined = Stream.of(c.addressLine(), c.city(), c.country())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? null : joined;
    }

    private ShipmentResponse toResponse(Shipment s) {
        Set<UUID> itemIds = s.getLines().stream().map(ShipmentLine::getItemId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) items.findAllByIds(itemIds).forEach(i -> itemMap.put(i.id(), i));
        return ShipmentMapper.toResponse(s, customers.getById(s.getCustomerId()).name(), itemMap);
    }

    private List<ShipmentSummaryResponse> toSummaries(List<Shipment> list) {
        List<UUID> ids = list.stream().map(Shipment::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (List<UUID> chunk : Chunks.partition(ids)) {
                shipments.countLines(chunk).forEach(c -> counts.put(c.getParentId(), c.getTotal()));
            }
        }
        Map<UUID, String> names = new HashMap<>();
        return list.stream().map(s -> ShipmentMapper.toSummary(s,
                names.computeIfAbsent(s.getCustomerId(), id -> customers.getById(id).name()),
                counts.getOrDefault(s.getId(), 0L).intValue())).toList();
    }
}

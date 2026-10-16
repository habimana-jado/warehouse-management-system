package rw.afriteck.wms.outbound.mappers;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.outbound.dtos.SalesOrderLineResponse;
import rw.afriteck.wms.outbound.dtos.SalesOrderResponse;
import rw.afriteck.wms.outbound.dtos.SalesOrderSummaryResponse;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;
import rw.afriteck.wms.outbound.repository.OrderTotals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SalesOrderMapper {
    private SalesOrderMapper() {}

    public static SalesOrderResponse toResponse(SalesOrder o, String customerName, Map<UUID, ItemResponse> items) {
        List<SalesOrderLineResponse> lines = o.getLines().stream()
                .map(l -> toLineResponse(l, items.get(l.getItemId())))
                .toList();
        return new SalesOrderResponse(o.getId(), o.getVersion(), o.getOrderNumber(),
                o.getCustomerId(), customerName, o.getWarehouseId(),
                o.getStatus(), o.getOrderDate(), o.getRequiredDate(), o.getCurrency(),
                o.getCustomerReference(), o.getNotes(), o.getCancelReason(),
                o.getConfirmedAt(), o.getClosedAt(), o.getCreatedAt(),
                orderedTotal(o), lines);
    }

    public static SalesOrderLineResponse toLineResponse(SalesOrderLine l, ItemResponse item) {
        return new SalesOrderLineResponse(l.getId(), l.getLineNo(), l.getItemId(),
                MapperSupport.sku(item), MapperSupport.name(item), MapperSupport.uom(item),
                l.getOrderedQty(), l.getReleasedQty(), l.getPickedQty(), l.getShippedQty(),
                l.unreleasedQty(), l.readyToShipQty(),
                l.getUnitPrice(), lineTotal(l), l.getNote());
    }

    /** totals may be null (no lines yet): all quantities then read as zero. */
    public static SalesOrderSummaryResponse toSummary(SalesOrder o, String customerName, OrderTotals totals) {
        return new SalesOrderSummaryResponse(o.getId(), o.getOrderNumber(), o.getCustomerId(), customerName,
                o.getWarehouseId(), o.getStatus(), o.getOrderDate(), o.getRequiredDate(), o.getCurrency(),
                totals == null ? 0 : (int) totals.getLineCount(),
                totals == null ? BigDecimal.ZERO : totals.getOrderedQty(),
                totals == null ? BigDecimal.ZERO : totals.getPickedQty(),
                totals == null ? BigDecimal.ZERO : totals.getShippedQty(),
                o.getCreatedAt());
    }

    static BigDecimal lineTotal(SalesOrderLine l) {
        return l.getUnitPrice() == null ? null
                : l.getOrderedQty().multiply(l.getUnitPrice()).setScale(4, RoundingMode.HALF_UP);
    }

    /** null when any line price is unknown: never summed as 0. */
    static BigDecimal orderedTotal(SalesOrder o) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SalesOrderLine l : o.getLines()) {
            BigDecimal t = lineTotal(l);
            if (t == null) return null;
            sum = sum.add(t);
        }
        return sum;
    }
}

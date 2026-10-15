package rw.afriteck.wms.inbound.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderLineResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderSummaryResponse;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PurchaseOrderMapper {

    private PurchaseOrderMapper() {}

    /** userNames: confirmedBy / closedBy display names; unknown ids are simply absent. */
    public static PurchaseOrderResponse toResponse(PurchaseOrder po, String supplierName,
                                                   Map<UUID, ItemResponse> items,
                                                   Map<UUID, String> userNames) {
        List<PurchaseOrderLineResponse> lines = po.getLines().stream()
                .map(l -> toLineResponse(l, items))
                .toList();
        return new PurchaseOrderResponse(
                po.getId(), po.getVersion(), po.getPoNumber(),
                po.getSupplierId(), supplierName,
                po.getWarehouseId(),
                po.getStatus(),
                po.getOrderDate(), po.getExpectedDate(),
                po.getSupplierReference(), po.getCurrency(), po.getNotes(),
                po.getConfirmedAt(), po.getConfirmedBy(), name(userNames, po.getConfirmedBy()),
                po.getClosedAt(), po.getClosedBy(), name(userNames, po.getClosedBy()),
                po.getCancelledAt(), po.getCancelReason(),
                po.getCreatedAt(),
                orderedTotal(po.getLines()),
                lines);
    }

    public static PurchaseOrderLineResponse toLineResponse(PurchaseOrderLine l, Map<UUID, ItemResponse> items) {
        ItemResponse item = items.get(l.getItemId());
        return new PurchaseOrderLineResponse(
                l.getId(), l.getLineNo(),
                l.getItemId(),
                item == null ? null : item.sku(),
                item == null ? null : item.name(),
                item == null ? null : item.baseUom(),
                l.getOrderedQty(), l.getAcceptedQty(), l.getRejectedQty(),
                l.remainingQty(),
                l.getUnitPrice(),
                l.getTolerancePercent(),
                l.acceptableQtyLeft(),
                l.getUnitPrice() == null ? null : lineTotal(l),
                l.getNotes());
    }

    /** Totals come from one grouped query for the whole page (see PurchaseOrderLineRepository.totalsByOrder). */
    public static PurchaseOrderSummaryResponse toSummary(PurchaseOrder po, String supplierName,
                                                         int lineCount, BigDecimal orderedQty, BigDecimal acceptedQty) {
        return new PurchaseOrderSummaryResponse(
                po.getId(), po.getPoNumber(),
                po.getSupplierId(), supplierName,
                po.getWarehouseId(),
                po.getStatus(),
                po.getOrderDate(), po.getExpectedDate(), po.getCurrency(),
                lineCount,
                orderedQty == null ? BigDecimal.ZERO : orderedQty,
                acceptedQty == null ? BigDecimal.ZERO : acceptedQty);
    }

    /** Null when there are no lines or any price is unknown: an unknown price is never treated as 0. */
    private static BigDecimal orderedTotal(List<PurchaseOrderLine> lines) {
        if (lines.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderLine l : lines) {
            if (l.getUnitPrice() == null) {
                return null;
            }
            total = total.add(lineTotal(l));
        }
        return total;
    }

    private static BigDecimal lineTotal(PurchaseOrderLine l) {
        return l.getOrderedQty().multiply(l.getUnitPrice()).setScale(4, RoundingMode.HALF_UP);
    }

    private static String name(Map<UUID, String> names, UUID id) {
        return id == null ? null : names.get(id);
    }
}
package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.inbound.dtos.PurchaseOrderSummaryResponse;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the purchase order board. */
public record PurchaseOrderRow(UUID id, String poNumber, String supplierName, PurchaseOrderStatus status,
                               LocalDate orderDate, LocalDate expectedDate, boolean overdue,
                               int lineCount, BigDecimal orderedQty, BigDecimal acceptedQty) {

    static PurchaseOrderRow of(PurchaseOrderSummaryResponse s, LocalDate today) {
        boolean waitingForGoods = s.status() == PurchaseOrderStatus.CONFIRMED
                || s.status() == PurchaseOrderStatus.PARTIALLY_RECEIVED;
        boolean overdue = waitingForGoods && s.expectedDate() != null && s.expectedDate().isBefore(today);
        return new PurchaseOrderRow(s.id(), s.poNumber(), s.supplierName(), s.status(),
                s.orderDate(), s.expectedDate(), overdue,
                s.lineCount(), s.orderedQty(), s.acceptedQty());
    }
}
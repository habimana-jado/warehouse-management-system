package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.SalesOrderSummaryResponse;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record SalesOrderRow(UUID id, String orderNumber, String customerName, SalesOrderStatus status,
                     LocalDate orderDate, LocalDate requiredDate, boolean overdue, int lineCount,
                     BigDecimal orderedQty, BigDecimal pickedQty, BigDecimal shippedQty) {

    static SalesOrderRow of(SalesOrderSummaryResponse s, LocalDate today) {
        boolean overdue = s.requiredDate() != null && s.requiredDate().isBefore(today)
                && SalesOrderStatus.OPEN.contains(s.status());
        return new SalesOrderRow(s.id(), s.orderNumber(), s.customerName(), s.status(), s.orderDate(),
                s.requiredDate(), overdue, s.lineCount(), s.orderedQty(), s.pickedQty(), s.shippedQty());
    }
}

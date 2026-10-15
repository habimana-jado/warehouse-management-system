package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseOrderSummaryResponse(
        UUID id, String poNumber,
        UUID supplierId, String supplierName,
        UUID warehouseId,
        PurchaseOrderStatus status,
        LocalDate orderDate, LocalDate expectedDate, String currency,
        int lineCount, BigDecimal orderedQty, BigDecimal acceptedQty) {}
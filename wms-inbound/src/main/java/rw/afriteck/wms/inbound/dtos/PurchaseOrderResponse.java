package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
        UUID id, long version, String poNumber,
        UUID supplierId, String supplierName,
        UUID warehouseId,
        PurchaseOrderStatus status,
        LocalDate orderDate, LocalDate expectedDate,
        String supplierReference, String currency, String notes,
        Instant confirmedAt, UUID confirmedBy, String confirmedByName,
        Instant closedAt, UUID closedBy, String closedByName,
        Instant cancelledAt, String cancelReason,
        Instant createdAt,
        BigDecimal orderedTotal,                   // null when any line has an unknown price (never counted as 0)
        List<PurchaseOrderLineResponse> lines) {}
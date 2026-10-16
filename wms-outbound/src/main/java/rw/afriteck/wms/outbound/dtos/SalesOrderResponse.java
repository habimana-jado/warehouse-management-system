package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SalesOrderResponse(UUID id, long version, String orderNumber,
                                 UUID customerId, String customerName, UUID warehouseId,
                                 SalesOrderStatus status, LocalDate orderDate, LocalDate requiredDate, String currency,
                                 String customerReference, String notes, String cancelReason,
                                 Instant confirmedAt, Instant closedAt, Instant createdAt,
                                 BigDecimal orderedTotal,// null when any line price is unknown
                                 List<SalesOrderLineResponse> lines) {}

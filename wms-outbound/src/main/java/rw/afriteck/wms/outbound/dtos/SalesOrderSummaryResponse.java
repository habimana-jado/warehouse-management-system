package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SalesOrderSummaryResponse(UUID id, String orderNumber, UUID customerId, String customerName,
                                        UUID warehouseId, SalesOrderStatus status, LocalDate orderDate, LocalDate requiredDate, String currency,
                                        int lineCount, BigDecimal orderedQty, BigDecimal pickedQty, BigDecimal shippedQty, Instant createdAt) {}

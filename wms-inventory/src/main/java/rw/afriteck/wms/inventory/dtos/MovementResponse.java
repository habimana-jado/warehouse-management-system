package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MovementResponse(
        UUID id, UUID transactionId, MovementType type, Instant occurredAt,
        UUID warehouseId, UUID locationId, UUID itemId,
        UUID lotId, String lotNumber, LocalDate expiryDate, String serialNumber,
        BigDecimal quantity, BigDecimal balanceAfter, BigDecimal unitCost,
        String referenceType, UUID referenceId, String referenceNumber, String note) {
}

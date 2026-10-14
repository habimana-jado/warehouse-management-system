package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.CountLineStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CountLineResponse(
        UUID id, int lineNo, UUID locationId, String locationCode,
        UUID itemId, String itemSku, String itemName,
        UUID lotId, String lotNumber, LocalDate expiryDate, String serialNumber,
        BigDecimal expectedQty, BigDecimal countedQty, BigDecimal variance,
        CountLineStatus status, UUID countedBy, String countedByName, Instant countedAt) {}

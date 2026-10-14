package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StockLevelResponse(
        UUID id, UUID warehouseId,
        UUID locationId, String locationCode, LocationUsage locationUsage,
        UUID itemId, String itemSku, String itemName, String baseUom,
        UUID lotId, String lotNumber, LocalDate expiryDate, String serialNumber,
        BigDecimal quantity) {
}

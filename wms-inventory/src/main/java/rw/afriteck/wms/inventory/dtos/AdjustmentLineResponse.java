package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AdjustmentLineResponse(
        UUID id, int lineNo, UUID locationId, String locationCode,
        UUID itemId, String itemSku, String itemName, String lotNumber, LocalDate expiryDate,
        String serialNumber, BigDecimal quantityDelta) {
}

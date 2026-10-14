package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PostStockLine(
        MovementType type,
        UUID locationId,                 // an active BIN
        UUID itemId,
        String lotNumber,                // normalized by the service
        LocalDate expiryDate,            // only used when the lot is created, or checked against the existing lot
        String serialNumber,             // normalized by the service
        BigDecimal quantity,             // SIGNED delta in the item's base UOM (sign rules below)
        BigDecimal unitCost) {           // optional; stored as given, null stays null

    public static PostStockLine receipt(UUID locationId, UUID itemId, String lotNumber, LocalDate expiryDate,
                                        String serialNumber, BigDecimal qty, BigDecimal unitCost) {
        return new PostStockLine(MovementType.RECEIPT, locationId, itemId, lotNumber, expiryDate,
                serialNumber, qty, unitCost);
    }

    /** Takes a POSITIVE quantity and posts it as a negative delta, so callers never handle the sign. */
    public static PostStockLine issue(UUID locationId, UUID itemId, String lotNumber, String serialNumber, BigDecimal qty) {
        return new PostStockLine(MovementType.ISSUE, locationId, itemId, lotNumber, null,
                serialNumber, qty.negate(), null);
    }
}

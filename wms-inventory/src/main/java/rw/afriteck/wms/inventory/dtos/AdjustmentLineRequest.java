package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AdjustmentLineRequest(
        UUID locationId, UUID itemId, String lotNumber, LocalDate expiryDate,
        String serialNumber, BigDecimal quantityDelta) {}      // signed, non-zero, base UOM

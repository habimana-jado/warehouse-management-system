package rw.afriteck.wms.foundation.dtos;

import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID warehouseId,
        UUID parentId,
        String code,
        LocationType type,
        LocationUsage usage,
        String barcode,
        BigDecimal maxCapacity,
        boolean active) {
}

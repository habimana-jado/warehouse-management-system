package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateLocationRequest(
        @NotNull UUID warehouseId,
        UUID parentId,
        @NotBlank @Size(max = 40) String code,
        @Size(max = 120) String name,
        @NotNull LocationType type,
        LocationUsage usage,                 // defaults to STORAGE
        @Size(max = 60) String barcode,      // generated when blank
        @PositiveOrZero BigDecimal maxCapacity) {
}

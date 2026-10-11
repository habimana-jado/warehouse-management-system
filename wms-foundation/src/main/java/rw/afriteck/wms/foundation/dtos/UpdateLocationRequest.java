package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;

public record UpdateLocationRequest(
        @Size(max = 120) String name,
        LocationUsage usage,
        @Size(max = 60) String barcode,
        @PositiveOrZero BigDecimal maxCapacity) {
}

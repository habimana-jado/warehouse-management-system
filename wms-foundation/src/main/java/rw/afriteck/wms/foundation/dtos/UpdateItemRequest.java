package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateItemRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @Size(max = 60) String barcode,
        UUID categoryId,
        @PositiveOrZero BigDecimal unitCost,
        @PositiveOrZero BigDecimal weightKg) {
}

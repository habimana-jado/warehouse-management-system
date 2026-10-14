package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import rw.afriteck.wms.foundation.enums.TrackingType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateItemRequest(
        @NotBlank @Size(max = 40) String sku,
        @NotBlank @Size(max = 200) String name,
        String description,
        String barcode,
        UUID categoryId,
        @NotBlank String baseUom,
        @NotNull TrackingType trackingType,
        @NotNull boolean expiryTracked,
        BigDecimal unitCost,
        @PositiveOrZero BigDecimal weightKg,
        List<ItemUomRequest> uoms) {
}

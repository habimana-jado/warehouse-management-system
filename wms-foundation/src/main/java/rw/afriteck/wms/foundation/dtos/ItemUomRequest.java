package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemUomRequest(
        @NotBlank String uom,
        @NotNull @Positive BigDecimal factorToBase,
        String barcode) {
}

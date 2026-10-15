package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Managers may change the tolerance of a line while the PO is still open (CONFIRMED / PARTIALLY_RECEIVED). */
public record ChangeToleranceRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal tolerancePercent) {}
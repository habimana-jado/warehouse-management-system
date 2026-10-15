package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineRequest(
        UUID id,                                   // update only: existing line; null = new line
        @NotNull UUID itemId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,   // in `uom`
        String uom,                                // null/blank = base UOM; a box UOM is converted by the service
        @DecimalMin("0") BigDecimal unitPrice,     // per `uom`; stored per BASE unit (line total preserved); null = unknown
        @DecimalMin("0") @DecimalMax("100") BigDecimal tolerancePercent,   // null -> 0
        @Size(max = 500) String notes) {}
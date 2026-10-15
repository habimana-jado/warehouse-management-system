package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerReturnLineRequest(
        @NotNull UUID itemId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,   // in `uom`
        String uom,                                // null/blank = base UOM
        @Size(max = 100) String lotNumber,
        LocalDate expiryDate,
        @Size(max = 100) String serialNumber,
        @Size(max = 500) String returnReason) {}
package rw.afriteck.wms.operations.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTaskLineRequest(
        @NotNull UUID itemId,
        @Size(max = 60) String lotNumber,
        @Size(max = 60) String serialNumber,
        UUID fromLocationId,
        UUID toLocationId,
        @NotNull @Positive BigDecimal plannedQty,    // item base UOM
        UUID referenceLineId
) {}

package rw.afriteck.wms.operations.dtos;

import jakarta.validation.constraints.NotNull;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.Instant;
import java.util.UUID;

public record ProductivityFilter(
        @NotNull UUID warehouseId,
        @NotNull Instant from,                       // inclusive, on completedAt
        @NotNull Instant to,                         // exclusive
        TaskType type                                // optional
) {}

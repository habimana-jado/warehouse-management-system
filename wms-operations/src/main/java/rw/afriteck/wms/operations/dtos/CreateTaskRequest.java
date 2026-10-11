package rw.afriteck.wms.operations.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull TaskType type,
        TaskPriority priority,                       // null -> NORMAL
        @NotNull UUID warehouseId,
        UUID zoneLocationId,
        @NotBlank @Size(max = 40) String referenceType,
        @NotNull UUID referenceId,
        @Size(max = 40) String referenceNumber,
        Instant dueAt,
        @Size(max = 500) String notes,
        @NotEmpty @Valid List<CreateTaskLineRequest> lines
) {
}

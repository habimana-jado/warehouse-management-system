package rw.afriteck.wms.operations.dtos;

import jakarta.validation.constraints.NotNull;
import rw.afriteck.wms.operations.enums.TaskPriority;

public record ChangePriorityRequest(@NotNull TaskPriority priority) {}


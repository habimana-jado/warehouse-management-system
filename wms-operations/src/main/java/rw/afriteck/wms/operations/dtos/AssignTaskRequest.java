package rw.afriteck.wms.operations.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTaskRequest(@NotNull UUID assigneeId) {}


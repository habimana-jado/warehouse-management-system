package rw.afriteck.wms.auth.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record AssignWarehousesRequest(@NotNull Set<UUID> warehouseIds) {}


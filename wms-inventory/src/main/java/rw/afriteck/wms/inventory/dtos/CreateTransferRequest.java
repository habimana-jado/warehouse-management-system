package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.operations.enums.TaskPriority;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateTransferRequest(
        TransferType type,
        UUID sourceWarehouseId,
        UUID destinationWarehouseId,        // BIN: null or equal to the source; INTER_WAREHOUSE: required
        boolean immediate,                  // BIN only: execute now. INTER_WAREHOUSE: must be false
        TaskPriority priority,              // planned BIN only, null -> NORMAL
        Instant dueAt,                      // planned BIN only
        String notes,
        List<CreateTransferLineRequest> lines) {}

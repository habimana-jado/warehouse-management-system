package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.MovementType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MovementFilter(
        UUID warehouseId,       // required
        UUID itemId, UUID locationId, UUID lotId, String serialNumber,
        Set<MovementType> types,
        Instant from, Instant to,          // from inclusive, to exclusive
        String referenceType, UUID referenceId) {}

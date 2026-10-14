package rw.afriteck.wms.inventory.dtos;

import java.util.UUID;

public record CreateCountRequest(UUID warehouseId, UUID zoneLocationId, String notes) {}   // zone optional: a ZONE of the warehouse


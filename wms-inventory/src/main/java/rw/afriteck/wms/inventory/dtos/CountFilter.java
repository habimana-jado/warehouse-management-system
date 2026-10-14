package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.CountStatus;

import java.util.Set;
import java.util.UUID;

public record CountFilter(UUID warehouseId, Set<CountStatus> statuses, String q) {}         // q: count number


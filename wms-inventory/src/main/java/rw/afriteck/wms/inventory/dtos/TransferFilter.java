package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.TransferStatus;

import java.util.Set;
import java.util.UUID;

public record TransferFilter(UUID warehouseId, Set<TransferStatus> statuses, String q) {}   // q: transfer number; matches source OR destination


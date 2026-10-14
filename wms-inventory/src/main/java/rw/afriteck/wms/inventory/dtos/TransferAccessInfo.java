package rw.afriteck.wms.inventory.dtos;

import rw.afriteck.wms.inventory.enums.TransferType;

import java.util.UUID;

public record TransferAccessInfo(UUID sourceWarehouseId, UUID destinationWarehouseId, TransferType type) {}


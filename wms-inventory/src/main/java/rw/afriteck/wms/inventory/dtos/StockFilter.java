package rw.afriteck.wms.inventory.dtos;

import java.util.UUID;

public record StockFilter(
        UUID warehouseId,          // required
        UUID locationId, UUID itemId, UUID lotId,
        String serialNumber,       // "where is this serial?"
        boolean includeZero) {}

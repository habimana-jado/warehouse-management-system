package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockBalance;

public final class StockLevelMapper {
    private StockLevelMapper() {}

    public static StockLevelResponse toResponse(StockBalance b, ItemResponse item, LocationResponse loc) {
        Lot lot = b.getLot();
        return new StockLevelResponse(b.getId(), b.getWarehouseId(),
                b.getLocationId(), loc == null ? null : loc.code(), loc == null ? null : loc.usage(),
                b.getItemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                item == null ? null : item.baseUom(),
                lot == null ? null : lot.getId(), lot == null ? null : lot.getLotNumber(),
                lot == null ? null : lot.getExpiryDate(), b.getSerialNumber(), b.getQuantity());
    }
}

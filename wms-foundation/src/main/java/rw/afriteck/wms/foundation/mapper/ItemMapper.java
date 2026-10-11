package rw.afriteck.wms.foundation.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponseDetailed;
import rw.afriteck.wms.foundation.dtos.ItemUomResponse;
import rw.afriteck.wms.foundation.entity.Item;
import rw.afriteck.wms.foundation.entity.ItemCategory;

public final class ItemMapper {
    private ItemMapper() {}

    public static ItemResponse toResponse(Item i) {
        ItemCategory c = i.getCategory();
        return new ItemResponse(i.getId(), i.getSku(), i.getName(), i.getBarcode(),
                c == null ? null : c.getId(), c == null ? null : c.getName(),
                i.getBaseUom(), i.getTrackingType(), i.isExpiryTracked(), i.isActive());
    }

    public static ItemResponseDetailed toDetailed(Item i) {
        ItemCategory c = i.getCategory();
        return new ItemResponseDetailed(i.getId(), i.getSku(), i.getName(), i.getDescription(),
                i.getBarcode(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                i.getBaseUom(), i.getTrackingType(), i.isExpiryTracked(),
                i.getUnitCost(), i.getWeightKg(), i.isActive(),
                i.getUoms().stream()
                        .map(u -> new ItemUomResponse(u.getId(), u.getUom(), u.getFactorToBase(), u.getBarcode()))
                        .toList(),
                i.getCreatedAt(), i.getUpdatedAt());
    }
}

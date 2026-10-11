package rw.afriteck.wms.foundation.dtos;

import rw.afriteck.wms.foundation.enums.TrackingType;

import java.util.UUID;

public record ItemResponse(
        UUID id, String sku, String name, String barcode,
        UUID categoryId, String categoryName, String baseUom,
        TrackingType trackingType, boolean expiryTracked, boolean active
) {
}

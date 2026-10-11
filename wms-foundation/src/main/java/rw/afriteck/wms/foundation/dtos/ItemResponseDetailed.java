package rw.afriteck.wms.foundation.dtos;

import rw.afriteck.wms.foundation.enums.TrackingType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ItemResponseDetailed(
        UUID id, String sku, String name, String description, String barcode,
        UUID categoryId, String categoryName, String baseUom,
        TrackingType trackingType, boolean expiryTracked,
        BigDecimal unitCost, BigDecimal weightKg, boolean active,
        List<ItemUomResponse> uoms, Instant createdAt, Instant updatedAt
) {
}

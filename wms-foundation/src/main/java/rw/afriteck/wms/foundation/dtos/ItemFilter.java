package rw.afriteck.wms.foundation.dtos;

import rw.afriteck.wms.foundation.enums.TrackingType;

import java.util.UUID;

public record ItemFilter(String q, UUID categoryId, TrackingType trackingType,
                         Boolean expiryTracked, Boolean active) {
    public ItemFilter {
        q = (q == null || q.isBlank()) ? null : q.trim();
    }
}
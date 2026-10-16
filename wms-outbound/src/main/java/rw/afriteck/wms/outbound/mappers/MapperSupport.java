package rw.afriteck.wms.outbound.mappers;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.*;
import rw.afriteck.wms.outbound.repository.OrderTotals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Package-private null-safe helpers shared by the outbound mappers. */
final class MapperSupport {
    private MapperSupport() {}

    static String sku(ItemResponse i)     { return i == null ? null : i.sku(); }
    static String name(ItemResponse i)    { return i == null ? null : i.name(); }
    static String uom(ItemResponse i)     { return i == null ? null : i.baseUom(); }
    static TrackingType tracking(ItemResponse i) { return i == null ? null : i.trackingType(); }
    static boolean expiryTracked(ItemResponse i) { return i != null && i.expiryTracked(); }
    static String locationCode(Map<UUID, LocationResponse> locations, UUID id) {
        if (id == null) return null;
        LocationResponse l = locations.get(id);
        return l == null ? null : l.code();
    }
}


package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.inventory.dtos.AdjustmentLineResponse;
import rw.afriteck.wms.inventory.dtos.AdjustmentResponse;
import rw.afriteck.wms.inventory.dtos.AdjustmentSummaryResponse;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;
import rw.afriteck.wms.inventory.entity.StockAdjustment;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdjustmentMapper {
    private AdjustmentMapper() {}

    public static AdjustmentResponse toResponse(StockAdjustment a, Map<UUID, String> names,
                                                Map<UUID, ItemResponse> items, Map<UUID, LocationResponse> locations) {
        List<AdjustmentLineResponse> lines = a.getLines().stream().map(l -> {
            ItemResponse item = items.get(l.getItemId());
            LocationResponse loc = locations.get(l.getLocationId());
            return new AdjustmentLineResponse(l.getId(), l.getLineNo(),
                    l.getLocationId(), loc == null ? null : loc.code(),
                    l.getItemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                    l.getLotNumber(), l.getExpiryDate(), l.getSerialNumber(), l.getQuantityDelta());
        }).toList();
        AdjustmentReason r = a.getReason();
        return new AdjustmentResponse(a.getId(), a.getVersion(), a.getAdjustmentNumber(), a.getWarehouseId(),
                r.getId(), r.getCode(), r.getName(), a.getStatus(), a.getNotes(),
                a.getRequestedBy(), name(names, a.getRequestedBy()),
                a.getDecidedBy(), name(names, a.getDecidedBy()),
                a.getDecidedAt(), a.getRejectionReason(), a.getCreatedAt(), lines);
    }

    public static AdjustmentSummaryResponse toSummary(StockAdjustment a, int lineCount, Map<UUID, String> names) {
        AdjustmentReason r = a.getReason();
        return new AdjustmentSummaryResponse(a.getId(), a.getAdjustmentNumber(), a.getWarehouseId(),
                r.getId(), r.getCode(), r.getName(), a.getStatus(),
                a.getRequestedBy(), name(names, a.getRequestedBy()), lineCount, a.getCreatedAt());
    }

    private static String name(Map<UUID, String> names, UUID id) {
        return id == null ? null : names.get(id);       // null-safe: immutable maps throw on get(null)
    }
}

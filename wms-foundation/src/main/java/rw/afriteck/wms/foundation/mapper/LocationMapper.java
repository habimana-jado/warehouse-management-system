package rw.afriteck.wms.foundation.mapper;

import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;
import rw.afriteck.wms.foundation.entity.Location;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LocationMapper {
    private LocationMapper() {}

    public static LocationResponse toResponse(Location l) {
        return new LocationResponse(l.getId(), l.getWarehouse().getId(),
                l.getParent() == null ? null : l.getParent().getId(),
                l.getCode(), l.getType(), l.getUsage(),
                l.getBarcode(), l.getMaxCapacity(), l.isActive());
    }

    public static LocationTreeResponse toNode(Location l, Map<UUID, List<Location>> childrenByParent) {
        List<LocationTreeResponse> kids = childrenByParent.getOrDefault(l.getId(), List.of()).stream()
                .map(c -> toNode(c, childrenByParent)).toList();
        return new LocationTreeResponse(l.getId(), l.getCode(), l.getName(), l.getType(),
                l.getUsage(), l.isActive(), kids);
    }
}

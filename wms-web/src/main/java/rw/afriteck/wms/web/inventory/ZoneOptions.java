package rw.afriteck.wms.web.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;

import java.util.*;

@Component
@RequiredArgsConstructor
public class ZoneOptions {

    private final LocationService locationService;

    public List<LocationResponse> forWarehouse(UUID warehouseId) {
        Set<UUID> parentIds = new HashSet<>();
        for (LocationUsage usage : LocationUsage.values()) {
            for (LocationResponse bin : locationService.findBinsByUsage(warehouseId, usage)) {
                if (bin.parentId() != null) {
                    parentIds.add(bin.parentId());
                }
            }
        }
        Map<UUID, LocationResponse> zones = new HashMap<>();
        Set<UUID> seen = new HashSet<>();
        for (int depth = 0; depth < 4 && !parentIds.isEmpty(); depth++) {      // ZONE > AISLE > RACK > BIN
            Set<UUID> next = new HashSet<>();
            for (LocationResponse l : locationService.findAllByIds(parentIds)) {
                if (!seen.add(l.id())) {
                    continue;
                }
                if (l.type() == LocationType.ZONE) {
                    if (l.active()) {
                        zones.put(l.id(), l);
                    }
                } else if (l.parentId() != null) {
                    next.add(l.parentId());
                }
            }
            parentIds = next;
        }
        return zones.values().stream().sorted(Comparator.comparing(LocationResponse::code)).toList();
    }
}

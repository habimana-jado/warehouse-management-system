package rw.afriteck.wms.reporting.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.SupplierService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Batch lookups into foundation (never one call per row). */
@Component
@RequiredArgsConstructor
public class ReportLookups {
    private final ItemService items;
    private final LocationService locations;
    private final SupplierService suppliers;
    private final WorkerLookup workers;

    public Map<UUID, ItemResponse> itemsById(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return items.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }
    public Map<UUID, LocationResponse> locationsById(Collection<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return locations.findAllByIds(ids).stream().collect(Collectors.toMap(LocationResponse::id, Function.identity()));
    }
    public Set<UUID> binIds(UUID warehouseId, LocationUsage usage) {
        return locations.findBinsByUsage(warehouseId, usage).stream().map(LocationResponse::id).collect(Collectors.toSet());
    }
    /** Lower-cased search text, or null when blank. */
    public static String needle(String q) {
        String s = Codes.blankToNull(q);
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }
    /** One getById per distinct id (SupplierService has no batch lookup, open decision 22). */
    public Map<UUID, SupplierResponse> suppliersById(Collection<UUID> ids) {
        Map<UUID, SupplierResponse> result = new HashMap<>();
        for (UUID id : new HashSet<>(ids)) {
            result.put(id, suppliers.getById(id));
        }
        return result;
    }
    public Map<UUID, String> workerNames(Collection<UUID> ids) {
        Set<UUID> clean = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return clean.isEmpty() ? Map.of() : workers.findDisplayNames(clean);
    }
}

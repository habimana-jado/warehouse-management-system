package rw.afriteck.wms.inbound.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;

import java.math.BigDecimal;
import java.util.*;

@Component
public class PutawaySuggester {

    /** Max bins inspected per item when looking for an empty one. */
    static final int EMPTY_BIN_SCAN_LIMIT = 25;

    private final InventoryQueryService inventoryQuery;
    private final LocationService locationService;

    public PutawaySuggester(InventoryQueryService inventoryQuery, LocationService locationService) {
        this.inventoryQuery = inventoryQuery;
        this.locationService = locationService;
    }

    /**
     * @return itemId -> suggested STORAGE bin id. Items are absent from the map only when the warehouse
     *         has no active STORAGE bin at all (the caller then asks the worker to pick one).
     */
    @Transactional(readOnly = true)
    public Map<UUID, UUID> suggest(UUID warehouseId, Collection<UUID> itemIds) {
        List<LocationResponse> bins = new ArrayList<>(
                locationService.findBinsByUsage(warehouseId, LocationUsage.STORAGE));
        if (bins.isEmpty() || itemIds.isEmpty()) {
            return Map.of();
        }
        bins.sort(Comparator.comparing(LocationResponse::code));

        Set<UUID> storageBinIds = new HashSet<>();
        for (LocationResponse b : bins) {
            storageBinIds.add(b.id());
        }

        Map<UUID, UUID> result = new LinkedHashMap<>();
        Set<UUID> taken = new HashSet<>();
        List<UUID> needEmptyBin = new ArrayList<>();

        // rule 1: where the item already sits
        for (UUID itemId : new LinkedHashSet<>(itemIds)) {
            Map<UUID, BigDecimal> perBin = new HashMap<>();
            for (StockLevelResponse s : inventoryQuery.findAvailable(warehouseId, itemId)) {
                if (storageBinIds.contains(s.locationId())) {
                    perBin.merge(s.locationId(), s.quantity(), BigDecimal::add);
                }
            }
            perBin.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresentOrElse(e -> result.put(itemId, e.getKey()),
                            () -> needEmptyBin.add(itemId));
        }

        // rules 2 and 3: new items
        for (UUID itemId : needEmptyBin) {
            UUID chosen = firstEmptyBin(warehouseId, bins, taken);
            if (chosen == null) {
                chosen = bins.get(0).id();
            }
            taken.add(chosen);
            result.put(itemId, chosen);
        }
        return result;
    }

    private UUID firstEmptyBin(UUID warehouseId, List<LocationResponse> bins, Set<UUID> taken) {
        int checked = 0;
        for (LocationResponse bin : bins) {
            if (taken.contains(bin.id())) {
                continue;
            }
            if (checked++ >= EMPTY_BIN_SCAN_LIMIT) {
                return null;
            }
            boolean empty = inventoryQuery.findInLocation(warehouseId, bin.id()).stream()
                    .noneMatch(s -> s.quantity().signum() > 0);
            if (empty) {
                return bin.id();
            }
        }
        return null;
    }
}
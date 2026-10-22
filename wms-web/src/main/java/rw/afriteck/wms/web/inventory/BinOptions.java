package rw.afriteck.wms.web.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BinOptions {

    public record BinOption(UUID id, String code, LocationUsage usage) {}

    private final LocationService locationService;

    /** Every active BIN of the warehouse sorted by code (findBinsByUsage returns active bins only). */
    public List<BinOption> forWarehouse(UUID warehouseId) {
        List<BinOption> all = new ArrayList<>();
        for (LocationUsage usage : LocationUsage.values()) {
            for (LocationResponse l : locationService.findBinsByUsage(warehouseId, usage)) {
                all.add(new BinOption(l.id(), l.code(), l.usage()));
            }
        }
        all.sort(Comparator.comparing(BinOption::code));
        return all;
    }
}

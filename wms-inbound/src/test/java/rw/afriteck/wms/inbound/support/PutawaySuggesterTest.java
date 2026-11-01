package rw.afriteck.wms.inbound.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PutawaySuggesterTest {

    @Mock
    private InventoryQueryService inventoryQuery;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private PutawaySuggester suggester;

    private UUID warehouseId;
    private UUID itemId;
    private UUID binId1;
    private UUID binId2;
    private LocationResponse bin1;
    private LocationResponse bin2;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        binId1 = UUID.randomUUID();
        binId2 = UUID.randomUUID();

        bin1 = new LocationResponse(binId1, warehouseId, null, "Bin A1", "A1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
        bin2 = new LocationResponse(binId2, warehouseId, null, "Bin A2", "A2", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
    }

    @Test
    void suggest_NoBins_ReturnsEmptyMap() {
        when(locationService.findBinsByUsage(warehouseId, LocationUsage.STORAGE)).thenReturn(List.of());

        Map<UUID, UUID> result = suggester.suggest(warehouseId, List.of(itemId));

        assertTrue(result.isEmpty());
    }

    @Test
    void suggest_ExistingStockLocation_SuggestsBinWithStock() {
        when(locationService.findBinsByUsage(warehouseId, LocationUsage.STORAGE)).thenReturn(List.of(bin1, bin2));

        StockLevelResponse stock = new StockLevelResponse(
                UUID.randomUUID(), warehouseId, binId2, "A2", LocationUsage.STORAGE,
                itemId, "SKU1", "Item 1", "PCS", null, null, null, null,
                BigDecimal.TEN);
        when(inventoryQuery.findAvailable(warehouseId, itemId)).thenReturn(List.of(stock));

        Map<UUID, UUID> result = suggester.suggest(warehouseId, List.of(itemId));

        assertEquals(1, result.size());
        assertEquals(binId2, result.get(itemId));
    }

    @Test
    void suggest_NewItem_SuggestsFirstEmptyBin() {
        when(locationService.findBinsByUsage(warehouseId, LocationUsage.STORAGE)).thenReturn(List.of(bin1, bin2));
        when(inventoryQuery.findAvailable(warehouseId, itemId)).thenReturn(List.of());
        when(inventoryQuery.findInLocation(warehouseId, binId1)).thenReturn(List.of());

        Map<UUID, UUID> result = suggester.suggest(warehouseId, List.of(itemId));

        assertEquals(1, result.size());
        assertEquals(binId1, result.get(itemId));
    }
}

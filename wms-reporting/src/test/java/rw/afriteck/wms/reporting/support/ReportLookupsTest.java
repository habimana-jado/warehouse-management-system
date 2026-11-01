package rw.afriteck.wms.reporting.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.SupplierService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportLookupsTest {

    @Mock
    private ItemService items;
    @Mock
    private LocationService locations;
    @Mock
    private SupplierService suppliers;
    @Mock
    private WorkerLookup workers;

    @InjectMocks
    private ReportLookups lookups;

    @Test
    void needle_formatting() {
        assertNull(ReportLookups.needle(null));
        assertNull(ReportLookups.needle("   "));
        assertEquals("test", ReportLookups.needle("  TEST  "));
    }

    @Test
    void itemsById_emptyAndNonEmpty() {
        assertTrue(lookups.itemsById(List.of()).isEmpty());

        UUID id = UUID.randomUUID();
        ItemResponse item = new ItemResponse(id, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.NONE, false, true);
        when(items.findAllByIds(List.of(id))).thenReturn(List.of(item));

        Map<UUID, ItemResponse> result = lookups.itemsById(List.of(id));
        assertEquals(1, result.size());
        assertEquals(item, result.get(id));
    }

    @Test
    void locationsById_emptyAndNonEmpty() {
        assertTrue(lookups.locationsById(List.of()).isEmpty());

        UUID id = UUID.randomUUID();
        LocationResponse loc = new LocationResponse(id, UUID.randomUUID(), null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
        when(locations.findAllByIds(List.of(id))).thenReturn(List.of(loc));

        Map<UUID, LocationResponse> result = lookups.locationsById(List.of(id));
        assertEquals(1, result.size());
        assertEquals(loc, result.get(id));
    }

    @Test
    void binIds() {
        UUID whId = UUID.randomUUID();
        UUID binId = UUID.randomUUID();
        LocationResponse loc = new LocationResponse(binId, whId, null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
        when(locations.findBinsByUsage(whId, LocationUsage.STORAGE)).thenReturn(List.of(loc));

        Set<UUID> result = lookups.binIds(whId, LocationUsage.STORAGE);
        assertEquals(Set.of(binId), result);
    }

    @Test
    void suppliersById() {
        UUID supId = UUID.randomUUID();
        SupplierResponse sup = new SupplierResponse(supId, "SUP1", "Supplier 1", "Contact", "123", "email@test.com", "Address", "Kigali", "Rwanda", true);
        when(suppliers.getById(supId)).thenReturn(sup);

        Map<UUID, SupplierResponse> result = lookups.suppliersById(List.of(supId, supId));
        assertEquals(1, result.size());
        assertEquals(sup, result.get(supId));
    }

    @Test
    void workerNames_nullAndNonNull() {
        assertTrue(lookups.workerNames(Collections.singletonList(null)).isEmpty());

        UUID wId = UUID.randomUUID();
        when(workers.findDisplayNames(Set.of(wId))).thenReturn(Map.of(wId, "John Doe"));

        Map<UUID, String> result = lookups.workerNames(List.of(wId));
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(wId));
    }
}

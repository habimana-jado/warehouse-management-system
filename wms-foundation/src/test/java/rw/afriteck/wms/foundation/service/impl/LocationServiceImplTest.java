package rw.afriteck.wms.foundation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.dtos.CreateLocationRequest;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;
import rw.afriteck.wms.foundation.entity.Location;
import rw.afriteck.wms.foundation.entity.Warehouse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.repository.LocationRepository;
import rw.afriteck.wms.foundation.repository.WarehouseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locations;

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private DeactivationGuards guards;

    private LocationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LocationServiceImpl(locations, warehouses, guards);
    }

    @Test
    void create_Success() {
        UUID whId = UUID.randomUUID();
        Warehouse wh = new Warehouse();
        wh.setId(whId);
        wh.setCode("WH-01");
        wh.setActive(true);

        CreateLocationRequest request = new CreateLocationRequest(
                whId, null, " A1-01 ", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, null, null
        );

        when(warehouses.findById(whId)).thenReturn(Optional.of(wh));
        when(locations.existsByWarehouseIdAndCode(whId, "A1-01")).thenReturn(false);

        Location saved = new Location();
        saved.setId(UUID.randomUUID());
        saved.setWarehouse(wh);
        saved.setCode("A1-01");
        saved.setType(LocationType.BIN);
        saved.setUsage(LocationUsage.STORAGE);
        saved.setBarcode("WH-01-A1-01");
        saved.setActive(true);

        when(locations.save(any(Location.class))).thenReturn(saved);

        LocationResponse res = service.create(request);

        assertNotNull(res);
        assertEquals("A1-01", res.code());
        assertEquals(LocationType.BIN, res.type());
        assertEquals(whId, res.warehouseId());
    }

    @Test
    void create_DuplicateCodeInWarehouse_ThrowsConflictException() {
        UUID whId = UUID.randomUUID();
        Warehouse wh = new Warehouse();
        wh.setId(whId);
        wh.setCode("WH-01");
        wh.setActive(true);

        CreateLocationRequest request = new CreateLocationRequest(
                whId, null, "A1-01", null, LocationType.BIN, LocationUsage.STORAGE, null, null
        );

        when(warehouses.findById(whId)).thenReturn(Optional.of(wh));
        when(locations.existsByWarehouseIdAndCode(whId, "A1-01")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertEquals("LOCATION_CODE_EXISTS", ex.getCode());
    }

    @Test
    void create_InvalidParentWarehouse_ThrowsBusinessException() {
        UUID whId = UUID.randomUUID();
        UUID parentWhId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();

        Warehouse wh = new Warehouse();
        wh.setId(whId);
        wh.setCode("WH-01");
        wh.setActive(true);

        Warehouse parentWh = new Warehouse();
        parentWh.setId(parentWhId);

        Location parentLoc = new Location();
        parentLoc.setId(parentId);
        parentLoc.setWarehouse(parentWh);

        CreateLocationRequest request = new CreateLocationRequest(
                whId, parentId, "A1-01", null, LocationType.BIN, LocationUsage.STORAGE, null, null
        );

        when(warehouses.findById(whId)).thenReturn(Optional.of(wh));
        when(locations.existsByWarehouseIdAndCode(whId, "A1-01")).thenReturn(false);
        when(locations.findById(parentId)).thenReturn(Optional.of(parentLoc));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals("PARENT_OTHER_WAREHOUSE", ex.getCode());
    }

    @Test
    void tree_Success() {
        UUID whId = UUID.randomUUID();
        Warehouse wh = new Warehouse();
        wh.setId(whId);

        Location zone = new Location();
        zone.setId(UUID.randomUUID());
        zone.setWarehouse(wh);
        zone.setCode("ZONE-A");
        zone.setType(LocationType.ZONE);

        Location bin = new Location();
        bin.setId(UUID.randomUUID());
        bin.setWarehouse(wh);
        bin.setCode("BIN-1");
        bin.setType(LocationType.BIN);
        bin.setParent(zone);

        when(locations.findByWarehouseIdOrderByCode(whId)).thenReturn(List.of(zone, bin));

        List<LocationTreeResponse> tree = service.tree(whId);

        assertEquals(1, tree.size());
        assertEquals("ZONE-A", tree.get(0).code());
        assertEquals(1, tree.get(0).children().size());
        assertEquals("BIN-1", tree.get(0).children().get(0).code());
    }

    @Test
    void deactivate_WithGuardCheck() {
        UUID id = UUID.randomUUID();
        Warehouse wh = new Warehouse();
        wh.setId(UUID.randomUUID());

        Location loc = new Location();
        loc.setId(id);
        loc.setWarehouse(wh);
        loc.setActive(true);

        when(locations.findById(id)).thenReturn(Optional.of(loc));

        LocationResponse res = service.deactivate(id);

        assertFalse(res.active());
        verify(guards).check(DeactivationGuard.Target.LOCATION, id);
    }
}

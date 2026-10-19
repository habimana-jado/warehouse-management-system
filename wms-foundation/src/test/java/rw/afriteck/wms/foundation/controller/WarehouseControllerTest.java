package rw.afriteck.wms.foundation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerTest {

    @Mock
    private WarehouseService service;

    @Mock
    private LocationService locationService;

    @Mock
    private UserContext userContext;

    private WarehouseController controller;

    @BeforeEach
    void setUp() {
        controller = new WarehouseController(service, locationService, userContext);
    }

    @Test
    void create_DelegatesToService() {
        CreateWarehouseRequest request = new CreateWarehouseRequest("WH1", "Warehouse 1", null, null, null);
        WarehouseResponse expected = new WarehouseResponse(UUID.randomUUID(), "WH1", "Warehouse 1", null, null, null, true);

        when(service.create(request)).thenReturn(expected);

        WarehouseResponse actual = controller.create(request);

        assertEquals(expected, actual);
        verify(service).create(request);
    }

    @Test
    void mine_DelegatesToService() {
        WarehouseResponse wh = new WarehouseResponse(UUID.randomUUID(), "WH1", "Warehouse 1", null, null, null, true);
        when(service.findAccessibleToCurrentUser()).thenReturn(List.of(wh));

        List<WarehouseResponse> actual = controller.mine();

        assertEquals(1, actual.size());
        assertEquals(wh, actual.get(0));
        verify(service).findAccessibleToCurrentUser();
    }

    @Test
    void search_DelegatesToService() {
        Pageable pageable = PageRequest.of(0, 20);
        WarehouseResponse wh = new WarehouseResponse(UUID.randomUUID(), "WH1", "Warehouse 1", null, null, null, true);
        Page<WarehouseResponse> page = new PageImpl<>(List.of(wh));

        when(service.search("query", pageable)).thenReturn(page);

        var response = controller.search("query", pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        verify(service).search("query", pageable);
    }

    @Test
    void get_ValidatesAccessAndDelegates() {
        UUID whId = UUID.randomUUID();
        WarehouseResponse expected = new WarehouseResponse(whId, "WH1", "Warehouse 1", null, null, null, true);

        when(service.getById(whId)).thenReturn(expected);

        WarehouseResponse actual = controller.get(whId);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(service).getById(whId);
    }

    @Test
    void searchLocationsByWarehouse_ValidatesAccessAndDelegates() {
        UUID whId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        LocationResponse loc = new LocationResponse(UUID.randomUUID(), whId, null, "Loc 1", "LOC1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);
        Page<LocationResponse> page = new PageImpl<>(List.of(loc));

        when(locationService.search(whId, "query", pageable)).thenReturn(page);

        var response = controller.searchLocationsByWarehouse(whId, "query", pageable);

        assertNotNull(response);
        assertEquals(1, response.content().size());
        verify(userContext).requireWarehouseAccess(whId);
        verify(locationService).search(whId, "query", pageable);
    }

    @Test
    void listWarehouseLocationsTree_ValidatesAccessAndDelegates() {
        UUID whId = UUID.randomUUID();
        LocationTreeResponse treeNode = new LocationTreeResponse(UUID.randomUUID(), "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, true, List.of());

        when(locationService.tree(whId)).thenReturn(List.of(treeNode));

        List<LocationTreeResponse> actual = controller.listWarehouseLocationsTree(whId);

        assertEquals(1, actual.size());
        verify(userContext).requireWarehouseAccess(whId);
        verify(locationService).tree(whId);
    }

    @Test
    void byBarcode_ValidatesAccessAndDelegates() {
        UUID whId = UUID.randomUUID();
        LocationResponse expected = new LocationResponse(UUID.randomUUID(), whId, null, "Loc 1", "LOC1", LocationType.BIN, LocationUsage.STORAGE, "BARCODE123", null, true);

        when(locationService.getByBarcode(whId, "BARCODE123")).thenReturn(expected);

        LocationResponse actual = controller.byBarcode(whId, "BARCODE123");

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(locationService).getByBarcode(whId, "BARCODE123");
    }

    @Test
    void update_ValidatesAccessAndDelegates() {
        UUID whId = UUID.randomUUID();
        UpdateWarehouseRequest request = new UpdateWarehouseRequest("Updated WH", null, null, null);
        WarehouseResponse expected = new WarehouseResponse(whId, "WH1", "Updated WH", null, null, null, true);

        when(service.update(whId, request)).thenReturn(expected);

        WarehouseResponse actual = controller.update(whId, request);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(service).update(whId, request);
    }

    @Test
    void activate_DelegatesToService() {
        UUID whId = UUID.randomUUID();
        WarehouseResponse expected = new WarehouseResponse(whId, "WH1", "Warehouse 1", null, null, null, true);

        when(service.activate(whId)).thenReturn(expected);

        WarehouseResponse actual = controller.activate(whId);

        assertEquals(expected, actual);
        verify(service).activate(whId);
    }

    @Test
    void deactivate_DelegatesToService() {
        UUID whId = UUID.randomUUID();
        WarehouseResponse expected = new WarehouseResponse(whId, "WH1", "Warehouse 1", null, null, null, false);

        when(service.deactivate(whId)).thenReturn(expected);

        WarehouseResponse actual = controller.deactivate(whId);

        assertEquals(expected, actual);
        verify(service).deactivate(whId);
    }
}

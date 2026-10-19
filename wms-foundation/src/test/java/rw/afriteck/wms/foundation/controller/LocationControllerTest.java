package rw.afriteck.wms.foundation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.CreateLocationRequest;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.UpdateLocationRequest;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private LocationService service;

    @Mock
    private UserContext userContext;

    private LocationController controller;

    @BeforeEach
    void setUp() {
        controller = new LocationController(service, userContext);
    }

    @Test
    void create_ValidatesAccessAndDelegates() {
        UUID whId = UUID.randomUUID();
        CreateLocationRequest request = new CreateLocationRequest(whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, null, null);
        LocationResponse expected = new LocationResponse(UUID.randomUUID(), whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);

        when(service.create(request)).thenReturn(expected);

        LocationResponse actual = controller.create(request);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(service).create(request);
    }

    @Test
    void get_ValidatesAccessAndDelegates() {
        UUID locId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        LocationResponse expected = new LocationResponse(locId, whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);

        when(service.getById(locId)).thenReturn(expected);

        LocationResponse actual = controller.get(locId);

        assertEquals(expected, actual);
        verify(service).getById(locId);
        verify(userContext).requireWarehouseAccess(whId);
    }

    @Test
    void update_ValidatesAccessAndDelegates() {
        UUID locId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        LocationResponse existing = new LocationResponse(locId, whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);
        UpdateLocationRequest request = new UpdateLocationRequest("Updated", LocationUsage.STORAGE, null, null);
        LocationResponse updated = new LocationResponse(locId, whId, null, "LOC1", "Updated", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);

        when(service.getById(locId)).thenReturn(existing);
        when(service.update(locId, request)).thenReturn(updated);

        LocationResponse actual = controller.update(locId, request);

        assertEquals(updated, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(service).update(locId, request);
    }

    @Test
    void activate_ValidatesAccessAndDelegates() {
        UUID locId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        LocationResponse existing = new LocationResponse(locId, whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, false);
        LocationResponse activated = new LocationResponse(locId, whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);

        when(service.getById(locId)).thenReturn(existing);
        when(service.activate(locId)).thenReturn(activated);

        LocationResponse actual = controller.activate(locId);

        assertEquals(activated, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(service).activate(locId);
    }

    @Test
    void deactivate_ValidatesAccessAndDelegates() {
        UUID locId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        LocationResponse existing = new LocationResponse(locId, whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, true);
        LocationResponse deactivated = new LocationResponse(locId, whId, null, "LOC1", "Loc 1", LocationType.BIN, LocationUsage.STORAGE, "LOC1", null, false);

        when(service.getById(locId)).thenReturn(existing);
        when(service.deactivate(locId)).thenReturn(deactivated);

        LocationResponse actual = controller.deactivate(locId);

        assertEquals(deactivated, actual);
        verify(userContext).requireWarehouseAccess(whId);
        verify(service).deactivate(locId);
    }
}

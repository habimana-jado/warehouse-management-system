package rw.afriteck.wms.foundation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.CreateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.UpdateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.entity.Warehouse;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.repository.WarehouseRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private DeactivationGuards guards;

    @Mock
    private UserContext userContext;

    private WarehouseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WarehouseServiceImpl(warehouses, guards, userContext);
    }

    @Test
    void create_Success() {
        CreateWarehouseRequest request = new CreateWarehouseRequest(" WH-01 ", "Warehouse One", "Address 1", "City", "Country");

        when(warehouses.existsByCodeIgnoreCase("WH-01")).thenReturn(false);

        Warehouse saved = new Warehouse();
        saved.setId(UUID.randomUUID());
        saved.setCode("WH-01");
        saved.setName("Warehouse One");
        saved.setAddressLine("Address 1");
        saved.setCity("City");
        saved.setCountry("Country");
        saved.setActive(true);

        when(warehouses.save(any(Warehouse.class))).thenReturn(saved);

        WarehouseResponse res = service.create(request);

        assertNotNull(res);
        assertEquals("WH-01", res.code());
        assertEquals("Warehouse One", res.name());
        assertTrue(res.active());
    }

    @Test
    void create_DuplicateCode_ThrowsConflictException() {
        CreateWarehouseRequest request = new CreateWarehouseRequest("WH-01", "Warehouse One", null, null, null);

        when(warehouses.existsByCodeIgnoreCase("WH-01")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertEquals("WAREHOUSE_CODE_EXISTS", ex.getCode());
    }

    @Test
    void update_Success() {
        UUID id = UUID.randomUUID();
        Warehouse existing = new Warehouse();
        existing.setId(id);
        existing.setCode("WH-01");
        existing.setName("Old Name");

        UpdateWarehouseRequest request = new UpdateWarehouseRequest("New Name", "New Addr", "New City", "New Country");

        when(warehouses.findById(id)).thenReturn(Optional.of(existing));

        WarehouseResponse res = service.update(id, request);

        assertEquals("New Name", res.name());
        assertEquals("New Addr", res.addressLine());
    }

    @Test
    void findAccessibleToCurrentUser_AdminReturnsAllActive() {
        when(userContext.isAdmin()).thenReturn(true);

        Warehouse wh = new Warehouse();
        wh.setId(UUID.randomUUID());
        wh.setCode("WH-01");
        wh.setName("Warehouse One");

        when(warehouses.findByActiveTrueOrderByName()).thenReturn(List.of(wh));

        List<WarehouseResponse> accessible = service.findAccessibleToCurrentUser();

        assertEquals(1, accessible.size());
        assertEquals("WH-01", accessible.get(0).code());
    }

    @Test
    void findAccessibleToCurrentUser_NonAdminReturnsAssigned() {
        UUID whId = UUID.randomUUID();
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.accessibleWarehouseIds()).thenReturn(Set.of(whId));

        Warehouse wh = new Warehouse();
        wh.setId(whId);
        wh.setCode("WH-01");
        wh.setName("Mine WH");

        when(warehouses.findByIdInAndActiveTrueOrderByName(Set.of(whId))).thenReturn(List.of(wh));

        List<WarehouseResponse> accessible = service.findAccessibleToCurrentUser();

        assertEquals(1, accessible.size());
        assertEquals("WH-01", accessible.get(0).code());
    }

    @Test
    void activate_Success() {
        UUID id = UUID.randomUUID();
        Warehouse wh = new Warehouse();
        wh.setId(id);
        wh.setCode("WH-01");
        wh.setActive(false);

        when(warehouses.findById(id)).thenReturn(Optional.of(wh));

        WarehouseResponse res = service.activate(id);

        assertTrue(res.active());
    }

    @Test
    void deactivate_WithGuardCheck() {
        UUID id = UUID.randomUUID();
        Warehouse wh = new Warehouse();
        wh.setId(id);
        wh.setCode("WH-01");
        wh.setActive(true);

        when(warehouses.findById(id)).thenReturn(Optional.of(wh));

        WarehouseResponse res = service.deactivate(id);

        assertFalse(res.active());
        verify(guards).check(DeactivationGuard.Target.WAREHOUSE, id);
    }
}

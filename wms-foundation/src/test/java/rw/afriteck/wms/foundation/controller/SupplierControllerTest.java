package rw.afriteck.wms.foundation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.foundation.dtos.CreateSupplierRequest;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.UpdateSupplierRequest;
import rw.afriteck.wms.foundation.service.SupplierService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    @Mock
    private SupplierService service;

    private SupplierController controller;

    @BeforeEach
    void setUp() {
        controller = new SupplierController(service);
    }

    @Test
    void create_DelegatesToService() {
        CreateSupplierRequest request = new CreateSupplierRequest("S1", "Supplier 1", null, null, null, null, null, null);
        SupplierResponse expected = new SupplierResponse(UUID.randomUUID(), "S1", "Supplier 1", null, null, null, null, null, null, true);

        when(service.create(request)).thenReturn(expected);

        SupplierResponse actual = controller.create(request);

        assertEquals(expected, actual);
        verify(service).create(request);
    }

    @Test
    void get_DelegatesToService() {
        UUID id = UUID.randomUUID();
        SupplierResponse expected = new SupplierResponse(id, "S1", "Supplier 1", null, null, null, null, null, null, true);

        when(service.getById(id)).thenReturn(expected);

        SupplierResponse actual = controller.get(id);

        assertEquals(expected, actual);
        verify(service).getById(id);
    }

    @Test
    void update_DelegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateSupplierRequest request = new UpdateSupplierRequest("Updated", null, null, null, null, null, null);
        SupplierResponse expected = new SupplierResponse(id, "S1", "Updated", null, null, null, null, null, null, true);

        when(service.update(id, request)).thenReturn(expected);

        SupplierResponse actual = controller.update(id, request);

        assertEquals(expected, actual);
        verify(service).update(id, request);
    }

    @Test
    void activate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        SupplierResponse expected = new SupplierResponse(id, "S1", "Supplier 1", null, null, null, null, null, null, true);

        when(service.activate(id)).thenReturn(expected);

        SupplierResponse actual = controller.activate(id);

        assertEquals(expected, actual);
        verify(service).activate(id);
    }

    @Test
    void deactivate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        SupplierResponse expected = new SupplierResponse(id, "S1", "Supplier 1", null, null, null, null, null, null, false);

        when(service.deactivate(id)).thenReturn(expected);

        SupplierResponse actual = controller.deactivate(id);

        assertEquals(expected, actual);
        verify(service).deactivate(id);
    }
}

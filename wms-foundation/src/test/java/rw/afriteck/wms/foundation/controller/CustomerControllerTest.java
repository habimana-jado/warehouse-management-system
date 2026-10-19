package rw.afriteck.wms.foundation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.foundation.dtos.CreateCustomerRequest;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.UpdateCustomerRequest;
import rw.afriteck.wms.foundation.service.CustomerService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService service;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(service);
    }

    @Test
    void create_DelegatesToService() {
        CreateCustomerRequest request = new CreateCustomerRequest("C1", "Customer 1", null, null, null, null, null, null);
        CustomerResponse expected = new CustomerResponse(UUID.randomUUID(), "C1", "Customer 1", null, null, null, null, null, null, true);

        when(service.create(request)).thenReturn(expected);

        CustomerResponse actual = controller.create(request);

        assertEquals(expected, actual);
        verify(service).create(request);
    }

    @Test
    void get_DelegatesToService() {
        UUID id = UUID.randomUUID();
        CustomerResponse expected = new CustomerResponse(id, "C1", "Customer 1", null, null, null, null, null, null, true);

        when(service.getById(id)).thenReturn(expected);

        CustomerResponse actual = controller.get(id);

        assertEquals(expected, actual);
        verify(service).getById(id);
    }

    @Test
    void update_DelegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateCustomerRequest request = new UpdateCustomerRequest("Updated", null, null, null, null, null, null);
        CustomerResponse expected = new CustomerResponse(id, "C1", "Updated", null, null, null, null, null, null, true);

        when(service.update(id, request)).thenReturn(expected);

        CustomerResponse actual = controller.update(id, request);

        assertEquals(expected, actual);
        verify(service).update(id, request);
    }

    @Test
    void activate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        CustomerResponse expected = new CustomerResponse(id, "C1", "Customer 1", null, null, null, null, null, null, true);

        when(service.activate(id)).thenReturn(expected);

        CustomerResponse actual = controller.activate(id);

        assertEquals(expected, actual);
        verify(service).activate(id);
    }

    @Test
    void deactivate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        CustomerResponse expected = new CustomerResponse(id, "C1", "Customer 1", null, null, null, null, null, null, false);

        when(service.deactivate(id)).thenReturn(expected);

        CustomerResponse actual = controller.deactivate(id);

        assertEquals(expected, actual);
        verify(service).deactivate(id);
    }
}

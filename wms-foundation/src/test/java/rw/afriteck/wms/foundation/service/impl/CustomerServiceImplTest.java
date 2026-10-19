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
import rw.afriteck.wms.foundation.dtos.CreateCustomerRequest;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.UpdateCustomerRequest;
import rw.afriteck.wms.foundation.entity.Customer;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.repository.CustomerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customers;

    @Mock
    private DeactivationGuards guards;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceImpl(customers, guards);
    }

    @Test
    void create_Success() {
        CreateCustomerRequest request = new CreateCustomerRequest(" CUST-01 ", "Customer One", "John Doe", "123456", "cust@test.com", "Address 1", "City", "Country");

        when(customers.existsByCodeIgnoreCase("CUST-01")).thenReturn(false);

        Customer saved = new Customer();
        saved.setId(UUID.randomUUID());
        saved.setCode("CUST-01");
        saved.setName("Customer One");
        saved.setContactPerson("John Doe");
        saved.setPhone("123456");
        saved.setEmail("cust@test.com");
        saved.setAddressLine("Address 1");
        saved.setCity("City");
        saved.setCountry("Country");
        saved.setActive(true);

        when(customers.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse res = service.create(request);

        assertNotNull(res);
        assertEquals("CUST-01", res.code());
        assertEquals("Customer One", res.name());
        assertTrue(res.active());
    }

    @Test
    void create_DuplicateCode_ThrowsConflictException() {
        CreateCustomerRequest request = new CreateCustomerRequest("CUST-01", "Customer One", null, null, null, null, null, null);

        when(customers.existsByCodeIgnoreCase("CUST-01")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertEquals("CUSTOMER_CODE_EXISTS", ex.getCode());
    }

    @Test
    void update_Success() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(id);
        existing.setCode("CUST-01");
        existing.setName("Old Name");

        UpdateCustomerRequest request = new UpdateCustomerRequest("New Name", "Jane Doe", "654321", "new@test.com", "New Address", "New City", "New Country");

        when(customers.findById(id)).thenReturn(Optional.of(existing));

        CustomerResponse res = service.update(id, request);

        assertEquals("New Name", res.name());
        assertEquals("Jane Doe", res.contactPerson());
    }

    @Test
    void update_NotFound_ThrowsNotFoundException() {
        UUID id = UUID.randomUUID();
        UpdateCustomerRequest request = new UpdateCustomerRequest("Name", null, null, null, null, null, null);

        when(customers.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(id, request));
    }

    @Test
    void getById_Success() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(id);
        existing.setCode("CUST-01");
        existing.setName("Customer One");

        when(customers.findById(id)).thenReturn(Optional.of(existing));

        CustomerResponse res = service.getById(id);

        assertEquals(id, res.id());
        assertEquals("CUST-01", res.code());
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCode("CUST-01");
        customer.setName("Customer One");

        when(customers.search("%cust%", pageable)).thenReturn(new PageImpl<>(List.of(customer)));

        Page<CustomerResponse> page = service.search("cust", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("CUST-01", page.getContent().get(0).code());
    }

    @Test
    void activate_Success() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCode("CUST-01");
        customer.setActive(false);

        when(customers.findById(id)).thenReturn(Optional.of(customer));

        CustomerResponse res = service.activate(id);

        assertTrue(res.active());
    }

    @Test
    void deactivate_WithGuardCheck() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCode("CUST-01");
        customer.setActive(true);

        when(customers.findById(id)).thenReturn(Optional.of(customer));

        CustomerResponse res = service.deactivate(id);

        assertFalse(res.active());
        verify(guards).check(DeactivationGuard.Target.CUSTOMER, id);
    }
}

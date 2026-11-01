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
import rw.afriteck.wms.foundation.dtos.CreateSupplierRequest;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.UpdateSupplierRequest;
import rw.afriteck.wms.foundation.entity.Supplier;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.repository.SupplierRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository suppliers;

    @Mock
    private DeactivationGuards guards;

    private SupplierServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SupplierServiceImpl(suppliers, guards);
    }

    @Test
    void create_Success() {
        CreateSupplierRequest request = new CreateSupplierRequest(" SUPP-01 ", "Supplier One", "John Doe", "123456", "supp@test.com", "Address 1", "City", "Country");

        when(suppliers.existsByCodeIgnoreCase("SUPP-01")).thenReturn(false);

        Supplier saved = new Supplier();
        saved.setId(UUID.randomUUID());
        saved.setCode("SUPP-01");
        saved.setName("Supplier One");
        saved.setContactPerson("John Doe");
        saved.setPhone("123456");
        saved.setEmail("supp@test.com");
        saved.setAddressLine("Address 1");
        saved.setCity("City");
        saved.setCountry("Country");
        saved.setActive(true);

        when(suppliers.save(any(Supplier.class))).thenReturn(saved);

        SupplierResponse res = service.create(request);

        assertNotNull(res);
        assertEquals("SUPP-01", res.code());
        assertEquals("Supplier One", res.name());
        assertTrue(res.active());
    }

    @Test
    void create_DuplicateCode_ThrowsConflictException() {
        CreateSupplierRequest request = new CreateSupplierRequest("SUPP-01", "Supplier One", null, null, null, null, null, null);

        when(suppliers.existsByCodeIgnoreCase("SUPP-01")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertEquals("SUPPLIER_CODE_EXISTS", ex.getCode());
    }

    @Test
    void update_Success() {
        UUID id = UUID.randomUUID();
        Supplier existing = new Supplier();
        existing.setId(id);
        existing.setCode("SUPP-01");
        existing.setName("Old Name");

        UpdateSupplierRequest request = new UpdateSupplierRequest("New Name", "Jane Doe", "654321", "new@test.com", "New Address", "New City", "New Country");

        when(suppliers.findById(id)).thenReturn(Optional.of(existing));

        SupplierResponse res = service.update(id, request);

        assertEquals("New Name", res.name());
        assertEquals("Jane Doe", res.contactPerson());
    }

    @Test
    void update_NotFound_ThrowsNotFoundException() {
        UUID id = UUID.randomUUID();
        UpdateSupplierRequest request = new UpdateSupplierRequest("Name", null, null, null, null, null, null);

        when(suppliers.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(id, request));
    }

    @Test
    void getById_Success() {
        UUID id = UUID.randomUUID();
        Supplier existing = new Supplier();
        existing.setId(id);
        existing.setCode("SUPP-01");
        existing.setName("Supplier One");

        when(suppliers.findById(id)).thenReturn(Optional.of(existing));

        SupplierResponse res = service.getById(id);

        assertEquals(id, res.id());
        assertEquals("SUPP-01", res.code());
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Supplier supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setCode("SUPP-01");
        supplier.setName("Supplier One");

        when(suppliers.search("%supp%", pageable)).thenReturn(new PageImpl<>(List.of(supplier)));

        Page<SupplierResponse> page = service.search("supp", pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("SUPP-01", page.getContent().get(0).code());
    }

    @Test
    void activate_Success() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setCode("SUPP-01");
        supplier.setActive(false);

        when(suppliers.findById(id)).thenReturn(Optional.of(supplier));

        SupplierResponse res = service.activate(id);

        assertTrue(res.active());
    }

    @Test
    void deactivate_WithGuardCheck() {
        UUID id = UUID.randomUUID();
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setCode("SUPP-01");
        supplier.setActive(true);

        when(suppliers.findById(id)).thenReturn(Optional.of(supplier));

        SupplierResponse res = service.deactivate(id);

        assertFalse(res.active());
        verify(guards).check(DeactivationGuard.Target.SUPPLIER, id);
    }
}

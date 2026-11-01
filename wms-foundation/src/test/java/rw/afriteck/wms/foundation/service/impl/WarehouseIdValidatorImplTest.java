package rw.afriteck.wms.foundation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.repository.WarehouseRepository;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseIdValidatorImplTest {

    @Mock
    private WarehouseRepository warehouses;

    private WarehouseIdValidatorImpl validator;

    @BeforeEach
    void setUp() {
        validator = new WarehouseIdValidatorImpl(warehouses);
    }

    @Test
    void validate_NullOrEmpty_DoesNothing() {
        validator.validate(null);
        validator.validate(Set.of());

        verifyNoInteractions(warehouses);
    }

    @Test
    void validate_AllExist_Success() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        when(warehouses.countByIdIn(ids)).thenReturn(2L);

        assertDoesNotThrow(() -> validator.validate(ids));
    }

    @Test
    void validate_MissingWarehouse_ThrowsBusinessException() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        when(warehouses.countByIdIn(ids)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> validator.validate(ids));
        assertEquals("UNKNOWN_WAREHOUSE", ex.getCode());
    }
}

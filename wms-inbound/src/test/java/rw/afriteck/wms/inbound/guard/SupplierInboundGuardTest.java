package rw.afriteck.wms.inbound.guard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierInboundGuardTest {

    @Mock
    private PurchaseOrderRepository purchaseOrders;

    @InjectMocks
    private SupplierInboundGuard guard;

    private UUID supplierId;

    @BeforeEach
    void setUp() {
        supplierId = UUID.randomUUID();
    }

    @Test
    void target_ReturnsSupplier() {
        assertEquals(DeactivationGuard.Target.SUPPLIER, guard.target());
    }

    @Test
    void assertCanDeactivate_Success() {
        when(purchaseOrders.existsBySupplierIdAndStatusIn(supplierId, PurchaseOrderStatus.OPEN)).thenReturn(false);

        assertDoesNotThrow(() -> guard.assertCanDeactivate(supplierId));
    }

    @Test
    void assertCanDeactivate_OpenOrders_ThrowsConflictException() {
        when(purchaseOrders.existsBySupplierIdAndStatusIn(supplierId, PurchaseOrderStatus.OPEN)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> guard.assertCanDeactivate(supplierId));
        assertEquals("SUPPLIER_HAS_OPEN_ORDERS", ex.getCode());
    }
}

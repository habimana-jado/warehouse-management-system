package rw.afriteck.wms.inbound.guard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.repository.CustomerReturnRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemInboundGuardTest {

    @Mock
    private PurchaseOrderRepository purchaseOrders;

    @Mock
    private CustomerReturnRepository customerReturns;

    @InjectMocks
    private ItemInboundGuard guard;

    private UUID itemId;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
    }

    @Test
    void target_ReturnsItem() {
        assertEquals(DeactivationGuard.Target.ITEM, guard.target());
    }

    @Test
    void assertCanDeactivate_Success() {
        when(purchaseOrders.existsByLines_ItemIdAndStatusIn(itemId, PurchaseOrderStatus.OPEN)).thenReturn(false);
        when(customerReturns.existsByLines_ItemIdAndStatusIn(itemId, Set.of(CustomerReturnStatus.EXPECTED))).thenReturn(false);

        assertDoesNotThrow(() -> guard.assertCanDeactivate(itemId));
    }

    @Test
    void assertCanDeactivate_OpenPurchaseOrders_ThrowsConflictException() {
        when(purchaseOrders.existsByLines_ItemIdAndStatusIn(itemId, PurchaseOrderStatus.OPEN)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> guard.assertCanDeactivate(itemId));
        assertEquals("ITEM_HAS_OPEN_ORDERS", ex.getCode());
    }

    @Test
    void assertCanDeactivate_ExpectedReturns_ThrowsConflictException() {
        when(purchaseOrders.existsByLines_ItemIdAndStatusIn(itemId, PurchaseOrderStatus.OPEN)).thenReturn(false);
        when(customerReturns.existsByLines_ItemIdAndStatusIn(itemId, Set.of(CustomerReturnStatus.EXPECTED))).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> guard.assertCanDeactivate(itemId));
        assertEquals("ITEM_HAS_EXPECTED_RETURNS", ex.getCode());
    }
}

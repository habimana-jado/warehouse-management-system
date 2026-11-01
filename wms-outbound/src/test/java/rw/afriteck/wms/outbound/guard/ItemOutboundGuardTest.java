package rw.afriteck.wms.outbound.guard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.repository.SupplierReturnRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemOutboundGuardTest {

    @Mock
    private SalesOrderRepository orders;

    @Mock
    private SupplierReturnRepository returns;

    @InjectMocks
    private ItemOutboundGuard guard;

    @Test
    void target_ReturnsItem() {
        assertEquals(DeactivationGuard.Target.ITEM, guard.target());
    }

    @Test
    void assertCanDeactivate_WhenNoOpenOrdersOrReturns_Succeeds() {
        UUID itemId = UUID.randomUUID();
        when(orders.existsByLines_ItemIdAndStatusIn(eq(itemId), eq(SalesOrderStatus.OPEN)))
                .thenReturn(false);
        when(returns.existsByLines_ItemIdAndStatusIn(eq(itemId), eq(SupplierReturnStatus.OPEN)))
                .thenReturn(false);

        assertDoesNotThrow(() -> guard.assertCanDeactivate(itemId));
    }

    @Test
    void assertCanDeactivate_WhenOpenSalesOrdersExist_ThrowsConflictException() {
        UUID itemId = UUID.randomUUID();
        when(orders.existsByLines_ItemIdAndStatusIn(eq(itemId), eq(SalesOrderStatus.OPEN)))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> guard.assertCanDeactivate(itemId));
        assertEquals("ITEM_HAS_OPEN_SALES_ORDERS", ex.getCode());
    }

    @Test
    void assertCanDeactivate_WhenDraftSupplierReturnsExist_ThrowsConflictException() {
        UUID itemId = UUID.randomUUID();
        when(orders.existsByLines_ItemIdAndStatusIn(eq(itemId), eq(SalesOrderStatus.OPEN)))
                .thenReturn(false);
        when(returns.existsByLines_ItemIdAndStatusIn(eq(itemId), eq(SupplierReturnStatus.OPEN)))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> guard.assertCanDeactivate(itemId));
        assertEquals("ITEM_HAS_DRAFT_SUPPLIER_RETURNS", ex.getCode());
    }
}

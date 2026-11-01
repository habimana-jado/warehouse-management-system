package rw.afriteck.wms.outbound.guard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOutboundGuardTest {

    @Mock
    private SalesOrderRepository orders;

    @InjectMocks
    private CustomerOutboundGuard guard;

    @Test
    void target_ReturnsCustomer() {
        assertEquals(DeactivationGuard.Target.CUSTOMER, guard.target());
    }

    @Test
    void assertCanDeactivate_WhenNoOpenOrders_Succeeds() {
        UUID customerId = UUID.randomUUID();
        when(orders.existsByCustomerIdAndStatusIn(eq(customerId), eq(SalesOrderStatus.OPEN)))
                .thenReturn(false);

        assertDoesNotThrow(() -> guard.assertCanDeactivate(customerId));
    }

    @Test
    void assertCanDeactivate_WhenOpenOrdersExist_ThrowsConflictException() {
        UUID customerId = UUID.randomUUID();
        when(orders.existsByCustomerIdAndStatusIn(eq(customerId), eq(SalesOrderStatus.OPEN)))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> guard.assertCanDeactivate(customerId));
        assertEquals("CUSTOMER_HAS_OPEN_ORDERS", ex.getCode());
    }
}

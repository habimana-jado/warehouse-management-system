package rw.afriteck.wms.outbound.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomerOutboundGuard implements DeactivationGuard {

    private final SalesOrderRepository orders;

    @Override public Target target() { return Target.CUSTOMER; }

    @Override
    public void assertCanDeactivate(UUID customerId) {
        if (orders.existsByCustomerIdAndStatusIn(customerId, SalesOrderStatus.OPEN)) {
            throw new ConflictException("CUSTOMER_HAS_OPEN_ORDERS", "The customer still has open sales orders");
        }
    }
}


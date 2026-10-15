package rw.afriteck.wms.inbound.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;

import java.util.UUID;

/** A supplier cannot be deactivated while it has open purchase orders. */
@Component
@RequiredArgsConstructor
public class SupplierInboundGuard implements DeactivationGuard {

    private final PurchaseOrderRepository purchaseOrders;

    @Override
    public Target target() {
        return Target.SUPPLIER;
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanDeactivate(UUID supplierId) {
        if (purchaseOrders.existsBySupplierIdAndStatusIn(supplierId, PurchaseOrderStatus.OPEN)) {
            throw new ConflictException("SUPPLIER_HAS_OPEN_ORDERS",
                    "The supplier still has open purchase orders (draft, confirmed or partially received)");
        }
    }
}
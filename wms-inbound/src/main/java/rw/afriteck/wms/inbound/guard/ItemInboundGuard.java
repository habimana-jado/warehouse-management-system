package rw.afriteck.wms.inbound.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.repository.CustomerReturnRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;

import java.util.Set;
import java.util.UUID;

/**
 * An item cannot be deactivated while it is on an open purchase order or on an expected customer return:
 * inventory refuses positive stock for inactive items, so those documents could never be received.
 */
@Component
@RequiredArgsConstructor
public class ItemInboundGuard implements DeactivationGuard {

    private final PurchaseOrderRepository purchaseOrders;
    private final CustomerReturnRepository customerReturns;

    @Override
    public Target target() {
        return Target.ITEM;
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanDeactivate(UUID itemId) {
        if (purchaseOrders.existsByLines_ItemIdAndStatusIn(itemId, PurchaseOrderStatus.OPEN)) {
            throw new ConflictException("ITEM_HAS_OPEN_ORDERS",
                    "The item is on an open purchase order (draft, confirmed or partially received)");
        }
        if (customerReturns.existsByLines_ItemIdAndStatusIn(itemId, Set.of(CustomerReturnStatus.EXPECTED))) {
            throw new ConflictException("ITEM_HAS_EXPECTED_RETURNS",
                    "The item is on a customer return that has not been received yet");
        }
    }
}
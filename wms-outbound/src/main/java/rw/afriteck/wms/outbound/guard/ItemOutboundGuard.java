package rw.afriteck.wms.outbound.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.repository.SupplierReturnRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemOutboundGuard implements DeactivationGuard {

    private final SalesOrderRepository orders;
    private final SupplierReturnRepository returns;

    @Override public Target target() { return Target.ITEM; }

    @Override
    public void assertCanDeactivate(UUID itemId) {
        if (orders.existsByLines_ItemIdAndStatusIn(itemId, SalesOrderStatus.OPEN)) {
            throw new ConflictException("ITEM_HAS_OPEN_SALES_ORDERS", "The item is on open sales orders");
        }
        if (returns.existsByLines_ItemIdAndStatusIn(itemId, SupplierReturnStatus.OPEN)) {
            throw new ConflictException("ITEM_HAS_DRAFT_SUPPLIER_RETURNS", "The item is on a draft supplier return");
        }
    }
}

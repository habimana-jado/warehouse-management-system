package rw.afriteck.wms.inventory.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.repository.StockAdjustmentRepository;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockCountRepository;
import rw.afriteck.wms.inventory.repository.StockTransferRepository;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseInventoryGuard implements DeactivationGuard {

    private static final Set<TransferStatus> OPEN_TRANSFERS = EnumSet.of(TransferStatus.PENDING, TransferStatus.IN_TRANSIT);
    private static final Set<CountStatus> OPEN_COUNTS = EnumSet.of(CountStatus.DRAFT, CountStatus.IN_PROGRESS, CountStatus.REVIEW);

    private final StockBalanceRepository balances;
    private final StockTransferRepository transfers;
    private final StockCountRepository counts;
    private final StockAdjustmentRepository adjustments;

    @Override
    public Target target() {
        return Target.WAREHOUSE;
    }

    @Override
    @Transactional(readOnly = true)
    public void assertCanDeactivate(UUID id) {
        if (balances.existsByWarehouseIdAndQuantityGreaterThan(id, BigDecimal.ZERO)) {
            throw new ConflictException("WAREHOUSE_HAS_STOCK", "The warehouse still holds stock");
        }
        if (transfers.countOpenForWarehouse(id, OPEN_TRANSFERS) > 0           // as source or as destination
                || counts.existsByWarehouseIdAndStatusIn(id, OPEN_COUNTS)
                || adjustments.existsByWarehouseIdAndStatus(id, AdjustmentStatus.PENDING_APPROVAL)) {
            throw new ConflictException("WAREHOUSE_HAS_OPEN_WORK",
                    "An open transfer, count or pending adjustment still involves this warehouse");
        }
    }
}

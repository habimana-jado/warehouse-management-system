package rw.afriteck.wms.inventory.support;

import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inventory.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class StockLineRules {
    public static final int SCALE = 4;
    private StockLineRules() {}

    /** lot and serial must already be normalized. Throws BusinessException. */
    public static void check(MovementType type, ItemResponse item, BigDecimal qty,
                             String lot, LocalDate expiry, String serial) {
        if (qty == null || qty.signum() == 0 || qty.stripTrailingZeros().scale() > SCALE) {
            throw new BusinessException("QTY_INVALID", "Quantity must be non-zero with at most " + SCALE + " decimals");
        }
        boolean signOk = switch (type) {
            case RECEIPT, TRANSFER_IN -> qty.signum() > 0;
            case ISSUE, TRANSFER_OUT -> qty.signum() < 0;
            case ADJUSTMENT, COUNT_ADJUSTMENT -> true;
        };
        if (!signOk) {
            throw new BusinessException("QTY_SIGN_INVALID", type + " does not allow a quantity of " + qty.toPlainString());
        }
        if (qty.signum() > 0 && !item.active()) {
            throw new BusinessException("ITEM_INACTIVE", "Item is inactive: " + item.sku());
        }
        switch (item.trackingType()) {
            case NONE -> {
                if (lot != null || serial != null || expiry != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is not tracked");
                }
            }
            case LOT -> {
                if (serial != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is lot tracked, not serial tracked");
                }
                if (lot == null) {
                    throw new BusinessException("LOT_REQUIRED", "Lot number required for item " + item.sku());
                }
                if (expiry != null && !item.expiryTracked()) {
                    throw new BusinessException("EXPIRY_NOT_ALLOWED", "Item " + item.sku() + " has no expiry tracking");
                }
            }
            case SERIAL -> {
                if (lot != null || expiry != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is serial tracked");
                }
                if (serial == null) {
                    throw new BusinessException("SERIAL_REQUIRED", "Serial number required for item " + item.sku());
                }
                if (qty.abs().compareTo(BigDecimal.ONE) != 0) {
                    throw new BusinessException("SERIAL_QTY_MUST_BE_ONE", "A serial moves one unit at a time");
                }
            }
        }
    }
}

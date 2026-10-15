package rw.afriteck.wms.inbound.support;

import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TrackingRules {

    private TrackingRules() {}

    /** @param totalQty total base quantity on the line (a SERIAL line must be exactly one unit). */
    public static void check(ItemResponse item, String lot, LocalDate expiry, String serial, BigDecimal totalQty) {
        switch (item.trackingType()) {
            case NONE -> {
                if (lot != null || serial != null || expiry != null) {
                    throw new BusinessException("TRACKING_MISMATCH",
                            "Item " + item.sku() + " is not lot or serial tracked: no lot, serial or expiry allowed");
                }
            }
            case LOT -> {
                if (serial != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is lot tracked: no serial allowed");
                }
                if (lot == null) {
                    throw new BusinessException("LOT_REQUIRED", "Item " + item.sku() + " needs a lot number");
                }
                if (item.expiryTracked() && expiry == null) {
                    throw new BusinessException("EXPIRY_REQUIRED", "Item " + item.sku() + " needs an expiry date");
                }
                if (!item.expiryTracked() && expiry != null) {
                    throw new BusinessException("EXPIRY_NOT_ALLOWED", "Item " + item.sku() + " does not track expiry");
                }
            }
            case SERIAL -> {
                if (lot != null || expiry != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is serial tracked: no lot or expiry allowed");
                }
                if (serial == null) {
                    throw new BusinessException("SERIAL_REQUIRED", "Item " + item.sku() + " needs a serial number");
                }
                if (totalQty.compareTo(BigDecimal.ONE) != 0) {
                    throw new BusinessException("SERIAL_QTY_MUST_BE_ONE",
                            "A serial line is exactly one unit (item " + item.sku() + ")");
                }
            }
        }
    }
}
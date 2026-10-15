package rw.afriteck.wms.inventory.support;

import rw.afriteck.wms.common.support.Codes;

public final class InventoryCodes {

    private InventoryCodes() {
    }

    public static String normalizeOrNull(String s) {
        String v = Codes.blankToNull(s);
        return v == null ? null : Codes.normalize(v);
    }
}

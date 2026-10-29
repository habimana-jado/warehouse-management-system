package rw.afriteck.wms.web.support;

import java.util.Map;
import java.util.UUID;

public final class AuditLinks {
    private AuditLinks() {
    }

    private static final Map<String, String> PATHS = Map.of(
            "STOCK_ADJUSTMENT", "/adjustments/",
            "STOCK_TRANSFER", "/transfers/",
            "STOCK_COUNT", "/counts/",
            "GOODS_RECEIPT", "/goods-receipts/",
            "PICK_LIST", "/pick-lists/",
            "RTV", "/supplier-returns/",
            "RMA", "/customer-returns/",
            "TASK", "/tasks/");

    public static String pathFor(String referenceType, UUID referenceId) {
        String base = referenceType == null ? null : PATHS.get(referenceType);
        return base == null || referenceId == null ? null : base + referenceId;
    }
}

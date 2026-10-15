package rw.afriteck.wms.inbound.enums;

import java.util.EnumSet;
import java.util.Set;

public enum PurchaseOrderStatus {
    DRAFT, CONFIRMED, PARTIALLY_RECEIVED, RECEIVED, CLOSED, CANCELLED;

    /** Not finished yet: used by the SUPPLIER and ITEM deactivation guards. */
    public static final Set<PurchaseOrderStatus> OPEN = EnumSet.of(DRAFT, CONFIRMED, PARTIALLY_RECEIVED);

    /** Goods may be received against the PO. */
    public static final Set<PurchaseOrderStatus> RECEIVABLE = EnumSet.of(CONFIRMED, PARTIALLY_RECEIVED);
}

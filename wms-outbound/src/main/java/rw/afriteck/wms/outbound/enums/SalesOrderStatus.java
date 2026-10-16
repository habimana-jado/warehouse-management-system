package rw.afriteck.wms.outbound.enums;

import java.util.EnumSet;
import java.util.Set;

public enum SalesOrderStatus {
    DRAFT, CONFIRMED, PARTIALLY_SHIPPED, SHIPPED, CLOSED, CANCELLED;
    public static final Set<SalesOrderStatus> OPEN = EnumSet.of(DRAFT, CONFIRMED, PARTIALLY_SHIPPED);   // guards
    public static final Set<SalesOrderStatus> RELEASABLE = EnumSet.of(CONFIRMED, PARTIALLY_SHIPPED);
}

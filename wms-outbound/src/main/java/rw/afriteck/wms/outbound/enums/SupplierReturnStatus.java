package rw.afriteck.wms.outbound.enums;

import java.util.EnumSet;
import java.util.Set;

public enum SupplierReturnStatus {
    DRAFT, POSTED, CANCELLED;
    public static final Set<SupplierReturnStatus> OPEN = EnumSet.of(DRAFT);   // guards
}

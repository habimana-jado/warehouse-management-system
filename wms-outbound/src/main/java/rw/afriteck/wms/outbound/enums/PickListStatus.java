package rw.afriteck.wms.outbound.enums;

import java.util.EnumSet;
import java.util.Set;

public enum PickListStatus {
    RELEASED, PICKING, PICKED, CANCELLED;      // RELEASED: nothing closed yet; PICKING: some lines closed; PICKED: all closed
    public static final Set<PickListStatus> OPEN = EnumSet.of(RELEASED, PICKING);
}

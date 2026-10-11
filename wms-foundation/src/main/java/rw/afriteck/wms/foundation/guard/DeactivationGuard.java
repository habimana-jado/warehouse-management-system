package rw.afriteck.wms.foundation.guard;

import java.util.UUID;

public interface DeactivationGuard {
    enum Target { WAREHOUSE, LOCATION, ITEM, SUPPLIER, CUSTOMER }

    Target target();

    /** Throw ConflictException when the record is still in use. */
    void assertCanDeactivate(UUID id);
}

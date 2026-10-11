package rw.afriteck.wms.common.security;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface WorkerLookup {

    /** True when the user exists, is active and has access to the warehouse. */
    boolean isAssignableToWarehouse(UUID userId, UUID warehouseId);

    /** Display names (full name, falling back to username) for the given user ids. Unknown ids are omitted. */
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);
}

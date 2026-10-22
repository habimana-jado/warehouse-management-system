package rw.afriteck.wms.common.security;

import rw.afriteck.wms.common.dtos.WorkerOption;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WorkerLookup {

    /** True when the user exists, is active and has access to the warehouse. */
    boolean isAssignableToWarehouse(UUID userId, UUID warehouseId);

    /** Display names (full name, falling back to username) for the given user ids. Unknown ids are omitted. */
    Map<UUID, String> findDisplayNames(Collection<UUID> userIds);

    /** Active users who may work tasks and have access to the warehouse (admin: any warehouse). Sorted by name. */
    List<WorkerOption> findAssignableWorkers(UUID warehouseId);
}

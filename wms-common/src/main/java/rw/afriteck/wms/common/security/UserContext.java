package rw.afriteck.wms.common.security;

import rw.afriteck.wms.common.exception.ForbiddenException;

import java.util.Set;
import java.util.UUID;

public interface UserContext {

    UUID currentUserId();
    String currentUsername();
    boolean hasRole(String role);
    boolean canAccessWarehouse(UUID warehouseId);
    Set<UUID> accessibleWarehouseIds();      // empty set + isAdmin() => all
    boolean isAdmin();

    default void requireWarehouseAccess(UUID warehouseId) {
        if (!canAccessWarehouse(warehouseId)) {
            throw new ForbiddenException("No access to warehouse " + warehouseId);
        }
    }
}

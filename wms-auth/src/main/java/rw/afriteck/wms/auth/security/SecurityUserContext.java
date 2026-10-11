package rw.afriteck.wms.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.common.security.UserContext;

import java.util.Set;
import java.util.UUID;

@Component
public class SecurityUserContext implements UserContext {

    private AuthenticatedUser principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthenticatedUser u)) {
            throw new IllegalStateException("No authenticated user");
        }
        return u;
    }

    @Override public UUID currentUserId()      { return principal().id(); }
    @Override public String currentUsername()  { return principal().username(); }
    @Override public boolean isAdmin()         { return principal().isAdmin(); }

    @Override
    public boolean hasRole(String role) {
        return principal().roles().contains(RoleName.valueOf(role));
    }

    @Override
    public boolean canAccessWarehouse(UUID warehouseId) {
        AuthenticatedUser u = principal();
        return u.isAdmin() || u.warehouseIds().contains(warehouseId);
    }

    @Override
    public Set<UUID> accessibleWarehouseIds() {
        return principal().warehouseIds();
    }
}

package rw.afriteck.wms.auth.security;

import rw.afriteck.wms.auth.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String username,
        Set<RoleName> roles,
        Set<UUID> warehouseIds)
{
    public boolean isAdmin() { return roles.contains(RoleName.ADMIN); }
}

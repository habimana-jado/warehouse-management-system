package rw.afriteck.wms.auth.dtos;

import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        boolean active,
        Set<RoleName> roles,
        Set<UUID> warehouseIds,
        Instant lastLoginAt)
{
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                u.isActive(), Set.copyOf(u.getRoles()), Set.copyOf(u.getWarehouseIds()),
                u.getLastLoginAt());
    }
}

package rw.afriteck.wms.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.auth.dtos.*;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.repository.UserRepository;
import rw.afriteck.wms.common.config.WarehouseIdValidator;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.UserContext;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final RefreshTokenService refreshTokens;
    private final ObjectProvider<WarehouseIdValidator> warehouseValidator;
    private final UserContext userContext;

    public UserResponse create(CreateUserRequest r) {
        String username = r.username().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("USERNAME_EXISTS", "Username already taken: " + username);
        }
        passwordPolicy.validate(r.password(), username);
        Set<UUID> warehouses = r.warehouseIds() == null ? Set.of() : r.warehouseIds();
        validateWarehouses(warehouses);

        User u = new User();
        u.setUsername(username);
        u.setFullName(r.fullName());
        u.setEmail(r.email());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setMustChangePassword(true);                      // admin-set password is temporary
        u.setRoles(new HashSet<>(r.roles()));
        u.setWarehouseIds(new HashSet<>(warehouses));
        return UserResponse.from(users.save(u));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String q, Pageable pageable) {
        return users.search(q, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(find(id));
    }

    public UserResponse update(UUID id, UpdateUserRequest r) {
        User u = find(id);
        u.setFullName(r.fullName());
        u.setEmail(r.email());
        return UserResponse.from(u);
    }

    public UserResponse assignRoles(UUID id, AssignRolesRequest r) {
        User u = find(id);
        boolean losingAdmin = u.getRoles().contains(RoleName.ADMIN) && !r.roles().contains(RoleName.ADMIN);
        if (losingAdmin) ensureAnotherActiveAdmin(u);
        u.setRoles(new HashSet<>(r.roles()));
        return UserResponse.from(u);
    }

    public UserResponse assignWarehouses(UUID id, AssignWarehousesRequest r) {
        validateWarehouses(r.warehouseIds());
        User u = find(id);
        u.setWarehouseIds(new HashSet<>(r.warehouseIds()));
        return UserResponse.from(u);
    }

    public UserResponse setActive(UUID id, boolean active) {
        User u = find(id);
        if (!active) {
            if (u.getId().equals(userContext.currentUserId())) {
                throw new BusinessException("SELF_DISABLE", "You cannot disable your own account");
            }
            if (u.getRoles().contains(RoleName.ADMIN)) ensureAnotherActiveAdmin(u);
            refreshTokens.revokeAllForUser(u.getId());      // cut off sessions at next refresh
        }
        u.setActive(active);
        return UserResponse.from(u);
    }

    public void resetPassword(UUID id, ResetPasswordRequest r) {
        User u = find(id);
        passwordPolicy.validate(r.newPassword(), u.getUsername());
        u.setPasswordHash(encoder.encode(r.newPassword()));
        u.setMustChangePassword(true);
        u.setFailedLoginAttempts(0);
        u.setLockedUntil(null);
        refreshTokens.revokeAllForUser(u.getId());
    }

    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private void validateWarehouses(Set<UUID> ids) {
        warehouseValidator.ifAvailable(v -> v.validate(ids));
    }

    private void ensureAnotherActiveAdmin(User target) {
        long activeAdmins = users.countActiveWithRole(RoleName.ADMIN);
        boolean targetCounts = target.isActive() && target.getRoles().contains(RoleName.ADMIN);
        if (targetCounts && activeAdmins <= 1) {
            throw new BusinessException("LAST_ADMIN", "At least one active admin is required");
        }
    }
}

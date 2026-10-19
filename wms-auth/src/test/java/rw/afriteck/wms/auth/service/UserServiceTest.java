package rw.afriteck.wms.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import rw.afriteck.wms.auth.dtos.*;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.repository.UserRepository;
import rw.afriteck.wms.common.config.WarehouseIdValidator;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.UserContext;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private RefreshTokenService refreshTokens;
    @Mock
    private ObjectProvider<WarehouseIdValidator> warehouseValidator;
    @Mock
    private UserContext userContext;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(users, encoder, passwordPolicy, refreshTokens, warehouseValidator, userContext);
    }

    @Test
    void create_Success() {
        UUID whId = UUID.randomUUID();
        CreateUserRequest req = new CreateUserRequest("NewUser ", "New User", "newuser@test.com", "Password123", Set.of(RoleName.VIEWER), Set.of(whId));

        when(users.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(encoder.encode("Password123")).thenReturn("encoded-pass");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUsername("newuser");
        savedUser.setFullName("New User");
        savedUser.setEmail("newuser@test.com");
        savedUser.setMustChangePassword(true);
        savedUser.setRoles(Set.of(RoleName.VIEWER));
        savedUser.setWarehouseIds(Set.of(whId));

        when(users.save(any(User.class))).thenReturn(savedUser);

        UserResponse response = userService.create(req);

        verify(passwordPolicy).validate("Password123", "newuser");
        assertNotNull(response);
        assertEquals("newuser", response.username());
        assertEquals("New User", response.fullName());
    }

    @Test
    void create_DuplicateUsername_ThrowsConflictException() {
        CreateUserRequest req = new CreateUserRequest("existinguser", "Existing User", "existing@test.com", "Password123", Set.of(RoleName.VIEWER), Set.of());

        when(users.existsByUsernameIgnoreCase("existinguser")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> userService.create(req));
        assertEquals("USERNAME_EXISTS", ex.getCode());
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("john");

        when(users.search("%john%", pageable)).thenReturn(new PageImpl<>(List.of(u)));

        Page<UserResponse> result = userService.search("john", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("john", result.getContent().get(0).username());
    }

    @Test
    void search_EmptyQuery_UsesWildcard() {
        Pageable pageable = PageRequest.of(0, 10);
        when(users.search("%", pageable)).thenReturn(new PageImpl<>(List.of()));

        Page<UserResponse> result = userService.search("   ", pageable);

        assertNotNull(result);
        verify(users).search("%", pageable);
    }

    @Test
    void get_Success() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);
        u.setUsername("john");

        when(users.findById(userId)).thenReturn(Optional.of(u));

        UserResponse res = userService.get(userId);

        assertEquals(userId, res.id());
        assertEquals("john", res.username());
    }

    @Test
    void get_NotFound_ThrowsNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.get(userId));
    }

    @Test
    void update_Success() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);
        u.setFullName("Old Name");
        u.setEmail("old@test.com");

        UpdateUserRequest req = new UpdateUserRequest("New Name", "new@test.com");

        when(users.findById(userId)).thenReturn(Optional.of(u));

        UserResponse res = userService.update(userId, req);

        assertEquals("New Name", res.fullName());
        assertEquals("new@test.com", res.email());
    }

    @Test
    void assignRoles_Success() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);
        u.setRoles(Set.of(RoleName.VIEWER));

        AssignRolesRequest req = new AssignRolesRequest(Set.of(RoleName.PICKER));

        when(users.findById(userId)).thenReturn(Optional.of(u));

        UserResponse res = userService.assignRoles(userId, req);

        assertTrue(res.roles().contains(RoleName.PICKER));
        assertFalse(res.roles().contains(RoleName.VIEWER));
    }

    @Test
    void assignRoles_RemovingLastAdmin_ThrowsBusinessException() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);
        u.setActive(true);
        u.setRoles(Set.of(RoleName.ADMIN));

        AssignRolesRequest req = new AssignRolesRequest(Set.of(RoleName.VIEWER));

        when(users.findById(userId)).thenReturn(Optional.of(u));
        when(users.countActiveWithRole(RoleName.ADMIN)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.assignRoles(userId, req));

        assertEquals("LAST_ADMIN", ex.getCode());
    }

    @Test
    void assignWarehouses_Success() {
        UUID userId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);

        AssignWarehousesRequest req = new AssignWarehousesRequest(Set.of(whId));

        when(users.findById(userId)).thenReturn(Optional.of(u));

        UserResponse res = userService.assignWarehouses(userId, req);

        assertTrue(res.warehouseIds().contains(whId));
    }

    @Test
    void setActive_DeactivateSelf_ThrowsBusinessException() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);

        when(users.findById(userId)).thenReturn(Optional.of(u));
        when(userContext.currentUserId()).thenReturn(userId);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.setActive(userId, false));

        assertEquals("SELF_DISABLE", ex.getCode());
    }

    @Test
    void setActive_DeactivateOtherAdmin_WhenOtherAdminsExist_Success() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        User u = new User();
        u.setId(targetUserId);
        u.setActive(true);
        u.setRoles(Set.of(RoleName.ADMIN));

        when(users.findById(targetUserId)).thenReturn(Optional.of(u));
        when(userContext.currentUserId()).thenReturn(currentUserId);
        when(users.countActiveWithRole(RoleName.ADMIN)).thenReturn(2L);

        UserResponse res = userService.setActive(targetUserId, false);

        assertFalse(res.active());
        verify(refreshTokens).revokeAllForUser(targetUserId);
    }

    @Test
    void resetPassword_Success() {
        UUID userId = UUID.randomUUID();
        User u = new User();
        u.setId(userId);
        u.setUsername("john");
        u.setFailedLoginAttempts(3);
        u.setMustChangePassword(false);

        ResetPasswordRequest req = new ResetPasswordRequest("NewPass123!");

        when(users.findById(userId)).thenReturn(Optional.of(u));
        when(encoder.encode("NewPass123!")).thenReturn("new-hash");

        userService.resetPassword(userId, req);

        verify(passwordPolicy).validate("NewPass123!", "john");
        assertEquals("new-hash", u.getPasswordHash());
        assertTrue(u.isMustChangePassword());
        assertEquals(0, u.getFailedLoginAttempts());
        assertNull(u.getLockedUntil());
        verify(refreshTokens).revokeAllForUser(userId);
    }
}

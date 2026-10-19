package rw.afriteck.wms.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.dtos.*;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.repository.UserRepository;
import rw.afriteck.wms.auth.security.JwtService;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.exception.UnauthorizedException;
import rw.afriteck.wms.common.security.UserContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private PasswordPolicy passwordPolicy;
    @Mock
    private JwtService jwt;
    @Mock
    private RefreshTokenService refreshTokens;
    @Mock
    private UserContext userContext;

    private AuthService authService;
    private Instant now;
    private SecurityProperties props;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-19T10:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneId.of("UTC"));

        props = new SecurityProperties(
                new SecurityProperties.Jwt("secret-key-32-chars-long-for-testing!!", "wms", Duration.ofMinutes(15), Duration.ofDays(7)),
                new SecurityProperties.Lockout(5, Duration.ofMinutes(15)),
                null
        );

        when(encoder.encode("not-a-real-password")).thenReturn("dummy-hash");

        authService = new AuthService(users, encoder, passwordPolicy, jwt, refreshTokens, props, userContext, fixedClock);
        authService.init();
    }

    @Test
    void login_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPasswordHash("hashed-password");
        user.setActive(true);
        user.setRoles(Set.of(RoleName.ADMIN));
        user.setWarehouseIds(Set.of());

        LoginRequest loginReq = new LoginRequest("testuser", "raw-password");
        ClientInfo client = new ClientInfo("Browser", "127.0.0.1");

        when(users.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(encoder.matches("raw-password", "hashed-password")).thenReturn(true);
        when(refreshTokens.issueNewFamily(userId, client)).thenReturn(
                new RefreshTokenService.Issued("raw-refresh-token", now.plus(Duration.ofDays(7)))
        );
        when(jwt.generateAccessToken(any())).thenReturn("access-jwt");

        TokenResponse response = authService.login(loginReq, client);

        assertNotNull(response);
        assertEquals("access-jwt", response.accessToken());
        assertEquals("raw-refresh-token", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("testuser", response.user().username());

        assertEquals(now, user.getLastLoginAt());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void login_UnknownUser_ThrowsBadCredentialsAndRunsDummyHashMatch() {
        LoginRequest loginReq = new LoginRequest("unknownuser", "password");
        ClientInfo client = new ClientInfo("Browser", "127.0.0.1");

        when(users.findByUsernameIgnoreCase("unknownuser")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(loginReq, client));

        assertEquals("BAD_CREDENTIALS", ex.getCode());
        verify(encoder).matches("password", "dummy-hash");
    }

    @Test
    void login_LockedUser_ThrowsUnauthorizedException() {
        User user = new User();
        user.setUsername("lockeduser");
        user.setLockedUntil(now.plus(Duration.ofMinutes(10)));

        LoginRequest loginReq = new LoginRequest("lockeduser", "password");

        when(users.findByUsernameIgnoreCase("lockeduser")).thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(loginReq, null));

        assertEquals("ACCOUNT_LOCKED", ex.getCode());
    }

    @Test
    void login_WrongPassword_RegistersFailedLoginAndThrows() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("correct-hash");

        LoginRequest loginReq = new LoginRequest("testuser", "wrong-password");

        when(users.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "correct-hash")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(loginReq, null));

        assertEquals("BAD_CREDENTIALS", ex.getCode());
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    void login_DisabledUser_ThrowsAccountDisabled() {
        User user = new User();
        user.setUsername("disableduser");
        user.setPasswordHash("hashed-password");
        user.setActive(false);

        LoginRequest loginReq = new LoginRequest("disableduser", "correct-password");

        when(users.findByUsernameIgnoreCase("disableduser")).thenReturn(Optional.of(user));
        when(encoder.matches("correct-password", "hashed-password")).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(loginReq, null));

        assertEquals("ACCOUNT_DISABLED", ex.getCode());
    }

    @Test
    void refresh_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setActive(true);
        user.setRoles(Set.of(RoleName.VIEWER));
        user.setWarehouseIds(Set.of());

        ClientInfo client = new ClientInfo("Agent", "1.2.3.4");
        RefreshTokenService.Issued nextIssued = new RefreshTokenService.Issued("next-refresh-token", now.plus(Duration.ofDays(7)));
        RefreshTokenService.Rotated rotated = new RefreshTokenService.Rotated(userId, nextIssued);

        when(refreshTokens.rotate("raw-refresh-token", client)).thenReturn(rotated);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(jwt.generateAccessToken(any())).thenReturn("new-jwt");

        TokenResponse response = authService.refresh("raw-refresh-token", client);

        assertNotNull(response);
        assertEquals("new-jwt", response.accessToken());
        assertEquals("next-refresh-token", response.refreshToken());
    }

    @Test
    void refresh_DisabledUser_RevokesAllAndThrows() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setActive(false);

        RefreshTokenService.Issued nextIssued = new RefreshTokenService.Issued("next-refresh-token", now.plus(Duration.ofDays(7)));
        RefreshTokenService.Rotated rotated = new RefreshTokenService.Rotated(userId, nextIssued);

        when(refreshTokens.rotate("raw-refresh-token", null)).thenReturn(rotated);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refresh("raw-refresh-token", null));

        assertEquals("ACCOUNT_DISABLED", ex.getCode());
        verify(refreshTokens).revokeAllForUser(userId);
    }

    @Test
    void logout_Success() {
        authService.logout("raw-refresh-token");
        verify(refreshTokens).revokeFamilyOf("raw-refresh-token");
    }

    @Test
    void logoutAll_Success() {
        UUID userId = UUID.randomUUID();
        when(userContext.currentUserId()).thenReturn(userId);

        authService.logoutAll();

        verify(refreshTokens).revokeAllForUser(userId);
    }

    @Test
    void me_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        when(userContext.currentUserId()).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        UserResponse res = authService.me();

        assertEquals("testuser", res.username());
    }

    @Test
    void me_UserNotFound_ThrowsNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userContext.currentUserId()).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.me());
    }

    @Test
    void changePassword_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setPasswordHash("old-hash");
        user.setMustChangePassword(true);

        ChangePasswordRequest req = new ChangePasswordRequest("old-pass", "NewPass123!");

        when(userContext.currentUserId()).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(encoder.encode("NewPass123!")).thenReturn("new-hash");

        authService.changePassword(req);

        verify(passwordPolicy).validate("NewPass123!", "testuser");
        assertEquals("new-hash", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
        verify(refreshTokens).revokeAllForUser(userId);
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsBusinessException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setPasswordHash("old-hash");

        ChangePasswordRequest req = new ChangePasswordRequest("wrong-old-pass", "NewPass123!");

        when(userContext.currentUserId()).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-old-pass", "old-hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.changePassword(req));

        assertEquals("WRONG_PASSWORD", ex.getCode());
    }
}

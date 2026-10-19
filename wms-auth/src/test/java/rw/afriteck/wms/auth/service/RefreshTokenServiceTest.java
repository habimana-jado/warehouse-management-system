package rw.afriteck.wms.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.dtos.ClientInfo;
import rw.afriteck.wms.auth.entity.RefreshToken;
import rw.afriteck.wms.auth.repository.RefreshTokenRepository;
import rw.afriteck.wms.common.exception.UnauthorizedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService refreshTokenService;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-19T10:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneId.of("UTC"));

        SecurityProperties props = new SecurityProperties(
                new SecurityProperties.Jwt(
                        "secret-key-32-chars-long-for-testing!!",
                        "wms",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7)
                ),
                new SecurityProperties.Lockout(5, Duration.ofMinutes(15)),
                null
        );

        refreshTokenService = new RefreshTokenService(repo, props, fixedClock);
    }

    @Test
    void issueNewFamily_Success() {
        UUID userId = UUID.randomUUID();
        ClientInfo client = new ClientInfo("TestAgent", "127.0.0.1");

        RefreshTokenService.Issued issued = refreshTokenService.issueNewFamily(userId, client);

        assertNotNull(issued.rawToken());
        assertEquals(now.plus(Duration.ofDays(7)), issued.expiresAt());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertNotNull(saved.getTokenHash());
        assertNotNull(saved.getFamilyId());
        assertEquals(now, saved.getCreatedAt());
        assertEquals("TestAgent", saved.getUserAgent());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void rotate_Success() {
        UUID userId = UUID.randomUUID();
        ClientInfo client = new ClientInfo("TestAgent", "127.0.0.1");

        RefreshToken existing = new RefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setUserId(userId);
        existing.setFamilyId("family-123");
        existing.setCreatedAt(now.minus(Duration.ofDays(1)));
        existing.setExpiresAt(now.plus(Duration.ofDays(6)));

        when(repo.findByTokenHash(any())).thenReturn(Optional.of(existing));

        RefreshTokenService.Rotated rotated = refreshTokenService.rotate("raw-refresh-token", client);

        assertEquals(userId, rotated.userId());
        assertNotNull(rotated.next().rawToken());
        assertEquals(now, existing.getRevokedAt());

        verify(repo).save(any(RefreshToken.class));
    }

    @Test
    void rotate_TokenNotFound_ThrowsUnauthorizedException() {
        when(repo.findByTokenHash(any())).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate("unknown-token", null));

        assertEquals("INVALID_REFRESH_TOKEN", ex.getCode());
    }

    @Test
    void rotate_AlreadyRevokedToken_TriggersFamilyRevocationAndThrows() {
        RefreshToken existing = new RefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setUserId(UUID.randomUUID());
        existing.setFamilyId("family-123");
        existing.setRevokedAt(now.minus(Duration.ofHours(1)));

        when(repo.findByTokenHash(any())).thenReturn(Optional.of(existing));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate("reused-token", null));

        assertEquals("REFRESH_TOKEN_REUSED", ex.getCode());
        verify(repo).revokeFamily("family-123", now);
    }

    @Test
    void rotate_ExpiredToken_ThrowsUnauthorizedException() {
        RefreshToken existing = new RefreshToken();
        existing.setId(UUID.randomUUID());
        existing.setUserId(UUID.randomUUID());
        existing.setFamilyId("family-123");
        existing.setCreatedAt(now.minus(Duration.ofDays(10)));
        existing.setExpiresAt(now.minus(Duration.ofDays(3)));

        when(repo.findByTokenHash(any())).thenReturn(Optional.of(existing));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> refreshTokenService.rotate("expired-token", null));

        assertEquals("REFRESH_TOKEN_EXPIRED", ex.getCode());
    }

    @Test
    void revokeFamilyOf_Success() {
        RefreshToken existing = new RefreshToken();
        existing.setFamilyId("family-xyz");

        when(repo.findByTokenHash(any())).thenReturn(Optional.of(existing));

        refreshTokenService.revokeFamilyOf("raw-token");

        verify(repo).revokeFamily("family-xyz", now);
    }

    @Test
    void revokeFamilyOf_UnknownToken_SilentlySucceeds() {
        when(repo.findByTokenHash(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> refreshTokenService.revokeFamilyOf("unknown-token"));
        verify(repo, never()).revokeFamily(any(), any());
    }

    @Test
    void revokeAllForUser_Success() {
        UUID userId = UUID.randomUUID();

        refreshTokenService.revokeAllForUser(userId);

        verify(repo).revokeAllForUser(userId, now);
    }

    @Test
    void purgeExpired_Success() {
        when(repo.deleteExpiredBefore(now.minus(Duration.ofDays(1)))).thenReturn(5);

        int count = refreshTokenService.purgeExpired();

        assertEquals(5, count);
        verify(repo).deleteExpiredBefore(now.minus(Duration.ofDays(1)));
    }
}

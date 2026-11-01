package rw.afriteck.wms.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.common.exception.UnauthorizedException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private Clock fixedClock;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-19T10:00:00Z");
        fixedClock = Clock.fixed(now, ZoneId.of("UTC"));

        SecurityProperties props = new SecurityProperties(
                new SecurityProperties.Jwt(
                        "32-byte-long-secret-key-for-testing-jwt-service",
                        "wms-auth-test",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7)
                ),
                new SecurityProperties.Lockout(5, Duration.ofMinutes(15)),
                null
        );

        jwtService = new JwtService(props, fixedClock);
    }

    @Test
    void generateAndParseToken_Success() {
        UUID userId = UUID.randomUUID();
        UUID whId = UUID.randomUUID();
        AuthenticatedUser originalUser = new AuthenticatedUser(
                userId,
                "testuser",
                Set.of(RoleName.ADMIN, RoleName.WAREHOUSE_MANAGER),
                Set.of(whId)
        );

        String token = jwtService.generateAccessToken(originalUser);
        assertNotNull(token);

        AuthenticatedUser parsedUser = jwtService.parse(token);

        assertEquals(originalUser.id(), parsedUser.id());
        assertEquals(originalUser.username(), parsedUser.username());
        assertEquals(originalUser.roles(), parsedUser.roles());
        assertEquals(originalUser.warehouseIds(), parsedUser.warehouseIds());
    }

    @Test
    void parse_ExpiredToken_ThrowsUnauthorizedException() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                "testuser",
                Set.of(RoleName.PICKER),
                Set.of()
        );

        String token = jwtService.generateAccessToken(user);

        // Advance clock beyond 15 minutes TTL
        Clock futureClock = Clock.fixed(now.plus(Duration.ofMinutes(20)), ZoneId.of("UTC"));
        SecurityProperties props = new SecurityProperties(
                new SecurityProperties.Jwt(
                        "32-byte-long-secret-key-for-testing-jwt-service",
                        "wms-auth-test",
                        Duration.ofMinutes(15),
                        Duration.ofDays(7)
                ),
                new SecurityProperties.Lockout(5, Duration.ofMinutes(15)),
                null
        );
        JwtService futureJwtService = new JwtService(props, futureClock);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> futureJwtService.parse(token));
        assertEquals("TOKEN_EXPIRED", ex.getCode());
    }

    @Test
    void parse_InvalidToken_ThrowsUnauthorizedException() {
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> jwtService.parse("invalid.jwt.token"));
        assertEquals("TOKEN_INVALID", ex.getCode());
    }

    @Test
    void parse_TamperedToken_ThrowsUnauthorizedException() {
        UUID userId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(userId, "testuser", Set.of(), Set.of());
        String token = jwtService.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> jwtService.parse(tamperedToken));
        assertEquals("TOKEN_INVALID", ex.getCode());
    }
}

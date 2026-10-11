package rw.afriteck.wms.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.common.exception.UnauthorizedException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtService {

    private final SecretKey key;
    private final SecurityProperties.Jwt cfg;
    private final Clock clock;

    public JwtService(SecurityProperties props, Clock clock) {
        this.cfg = props.jwt();
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(cfg.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(AuthenticatedUser u) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(cfg.issuer())
                .subject(u.id().toString())
                .id(UUID.randomUUID().toString())
                .claim("username", u.username())
                .claim("roles", u.roles().stream().map(Enum::name).toList())
                .claim("wh", u.warehouseIds().stream().map(UUID::toString).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(cfg.accessTtl())))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(cfg.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Set<RoleName> roles = ((List<?>) c.get("roles", List.class)).stream()
                    .map(r -> RoleName.valueOf(r.toString()))
                    .collect(Collectors.toSet());
            Set<UUID> warehouses = ((List<?>) c.get("wh", List.class)).stream()
                    .map(w -> UUID.fromString(w.toString()))
                    .collect(Collectors.toSet());

            return new AuthenticatedUser(UUID.fromString(c.getSubject()),
                    c.get("username", String.class), roles, warehouses);

        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("TOKEN_EXPIRED", "Access token expired");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("TOKEN_INVALID", "Invalid access token");
        }
    }
}

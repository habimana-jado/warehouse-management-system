package rw.afriteck.wms.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.dtos.ClientInfo;
import rw.afriteck.wms.auth.entity.RefreshToken;
import rw.afriteck.wms.auth.repository.RefreshTokenRepository;
import rw.afriteck.wms.common.exception.UnauthorizedException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final SecurityProperties props;
    private final Clock clock;

    public record Issued(String rawToken, Instant expiresAt) {}
    public record Rotated(UUID userId, Issued next) {}

    @Transactional
    public Issued issueNewFamily(UUID userId, ClientInfo client) {
        return issue(userId, UUID.randomUUID().toString(), client);
    }

    /** noRollbackFor: the family revocation on reuse must be committed even though we throw. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotated rotate(String rawToken, ClientInfo client) {
        Instant now = clock.instant();

        RefreshToken current = repo.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException(
                        "INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (current.getRevokedAt() != null) {
            repo.revokeFamily(current.getFamilyId(), now);     // reuse detected: kill the whole family
            throw new UnauthorizedException("REFRESH_TOKEN_REUSED",
                    "Refresh token already used. Please log in again.");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Refresh token expired");
        }

        current.setRevokedAt(now);
        return new Rotated(current.getUserId(), issue(current.getUserId(), current.getFamilyId(), client));
    }

    @Transactional
    public void revokeFamilyOf(String rawToken) {
        repo.findByTokenHash(hash(rawToken))
                .ifPresent(t -> repo.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        repo.revokeAllForUser(userId, clock.instant());
    }

    @Transactional
    public int purgeExpired() {
        return repo.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
    }

    private Issued issue(UUID userId, String familyId, ClientInfo client) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = clock.instant();
        RefreshToken t = new RefreshToken();
        t.setUserId(userId);
        t.setTokenHash(hash(raw));
        t.setFamilyId(familyId);
        t.setCreatedAt(now);
        t.setExpiresAt(now.plus(props.jwt().refreshTtl()));
        if (client != null) {
            t.setUserAgent(client.userAgent());
            t.setIpAddress(client.ipAddress());
        }
        repo.save(t);
        return new Issued(raw, t.getExpiresAt());
    }

    private static String hash(String raw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

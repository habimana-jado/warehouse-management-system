package rw.afriteck.wms.auth.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.dtos.*;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.repository.UserRepository;
import rw.afriteck.wms.auth.security.AuthenticatedUser;
import rw.afriteck.wms.auth.security.JwtService;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.exception.UnauthorizedException;
import rw.afriteck.wms.common.security.UserContext;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwt;
    private final RefreshTokenService refreshTokens;
    private final SecurityProperties props;
    private final UserContext userContext;
    private final Clock clock;

    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = encoder.encode("not-a-real-password");   // equalizes timing for unknown usernames
    }

    /** noRollbackFor: failed-attempt counters must be saved even though we throw. */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse login(LoginRequest req, ClientInfo client) {
        Instant now = clock.instant();

        User user = users.findByUsernameIgnoreCase(req.username().trim()).orElse(null);
        if (user == null) {
            encoder.matches(req.password(), dummyHash);
            throw badCredentials();
        }
        if (user.isLocked(now)) {
            throw new UnauthorizedException("ACCOUNT_LOCKED",
                    "Too many failed attempts. Try again later.");
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            var lockout = props.lockout();
            user.registerFailedLogin(lockout.maxAttempts(), lockout.duration(), now);
            throw badCredentials();
        }
        if (!user.isActive()) {
            // checked after the password on purpose
            throw new UnauthorizedException("ACCOUNT_DISABLED", "Account is disabled");
        }

        user.registerSuccessfulLogin(now);
        return buildResponse(user, refreshTokens.issueNewFamily(user.getId(), client));
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(String rawRefreshToken, ClientInfo client) {
        var rotated = refreshTokens.rotate(rawRefreshToken, client);

        User user = users.findById(rotated.userId())
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        if (!user.isActive()) {
            refreshTokens.revokeAllForUser(user.getId());
            throw new UnauthorizedException("ACCOUNT_DISABLED", "Account is disabled");
        }
        return buildResponse(user, rotated.next());      // roles/warehouses are re-read here, so changes apply
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.revokeFamilyOf(rawRefreshToken);   // unknown token: silently succeed
    }

    @Transactional
    public void logoutAll() {
        refreshTokens.revokeAllForUser(userContext.currentUserId());
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return users.findById(userContext.currentUserId())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User", userContext.currentUserId()));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = users.findById(userContext.currentUserId())
                .orElseThrow(() -> new NotFoundException("User", userContext.currentUserId()));

        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("WRONG_PASSWORD", "Current password is incorrect");
        }
        passwordPolicy.validate(req.newPassword(), user.getUsername());
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        refreshTokens.revokeAllForUser(user.getId());    // all devices must log in again
    }

    private TokenResponse buildResponse(User user, RefreshTokenService.Issued refresh) {
        var principal = new AuthenticatedUser(user.getId(), user.getUsername(),
                Set.copyOf(user.getRoles()), Set.copyOf(user.getWarehouseIds()));
        Instant now = clock.instant();
        return new TokenResponse(
                jwt.generateAccessToken(principal),
                "Bearer",
                props.jwt().accessTtl().toSeconds(),
                refresh.rawToken(),
                Duration.between(now, refresh.expiresAt()).toSeconds(),
                user.isMustChangePassword(),
                UserResponse.from(user));
    }

    private UnauthorizedException badCredentials() {
        return new UnauthorizedException("BAD_CREDENTIALS", "Invalid username or password");
    }
}

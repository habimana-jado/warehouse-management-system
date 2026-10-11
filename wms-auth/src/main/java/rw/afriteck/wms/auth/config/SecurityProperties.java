package rw.afriteck.wms.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "wms.security")
@Validated
public record SecurityProperties(
        @Valid @NotNull Jwt jwt,
        @DefaultValue Lockout lockout,
        @DefaultValue Bootstrap bootstrap) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT secret must be at least 32 characters") String secret,
            @DefaultValue("wms") String issuer,
            @DefaultValue("15m") Duration accessTtl,
            @DefaultValue("7d") Duration refreshTtl) {}

    public record Lockout(
            @DefaultValue("5") int maxAttempts,
            @DefaultValue("15m") Duration duration) {}

    public record Bootstrap(String adminUsername, String adminPassword) {}
}

package rw.afriteck.wms.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
public class User extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String username;                 // stored lowercase

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String fullName;

    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean mustChangePassword = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;
    private Instant lastLoginAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<RoleName> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_warehouses", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "warehouse_id", nullable = false)
    private Set<UUID> warehouseIds = new HashSet<>();

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(int maxAttempts, Duration lockFor, Instant now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = now.plus(lockFor);
            failedLoginAttempts = 0;
        }
    }

    public void registerSuccessfulLogin(Instant now) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }
}

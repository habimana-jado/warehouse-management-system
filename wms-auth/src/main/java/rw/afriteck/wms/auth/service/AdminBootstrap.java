package rw.afriteck.wms.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.auth.config.SecurityProperties;
import rw.afriteck.wms.auth.entity.User;
import rw.afriteck.wms.auth.enums.RoleName;
import rw.afriteck.wms.auth.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordPolicy policy;
    private final SecurityProperties props;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var cfg = props.bootstrap();
        if (users.count() > 0 || cfg.adminPassword() == null || cfg.adminPassword().isBlank()) return;

        String username = cfg.adminUsername() == null ? "admin" : cfg.adminUsername().toLowerCase();
        policy.validate(cfg.adminPassword(), username);

        User admin = new User();
        admin.setUsername(username);
        admin.setFullName("System Administrator");
        admin.setPasswordHash(encoder.encode(cfg.adminPassword()));
        admin.setMustChangePassword(true);
        admin.setRoles(new HashSet<>(Set.of(RoleName.ADMIN)));
        users.save(admin);
        log.info("Created initial admin user '{}'", username);
    }
}

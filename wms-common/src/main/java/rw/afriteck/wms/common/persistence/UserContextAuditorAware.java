package rw.afriteck.wms.common.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.security.UserContext;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserContextAuditorAware implements AuditorAware<UUID> {
    private final ObjectProvider<UserContext> userContext;

    @Override
    public Optional<UUID> getCurrentAuditor() {
        try {
            UserContext ctx = userContext.getIfAvailable();
            return Optional.ofNullable(ctx).map(UserContext::currentUserId);
        } catch (Exception e) {
            return Optional.empty();   // startup, migrations, bootstrap admin creation
        }
    }
}
package rw.afriteck.wms.common.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.security.UserContext;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserContextAuditorAware implements AuditorAware<String> {
    private final ObjectProvider<UserContext> userContext;

    @Override
    public Optional<String> getCurrentAuditor() {
        UserContext ctx = userContext.getIfAvailable();
        try {
            return Optional.ofNullable(ctx).map(UserContext::currentUsername);
        } catch (Exception e) {
            return Optional.of("system");   // startup, migrations, scheduled jobs
        }
    }
}
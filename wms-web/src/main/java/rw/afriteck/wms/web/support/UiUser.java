package rw.afriteck.wms.web.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.security.UserContext;

import java.util.Arrays;

@Component("uiUser")
@RequiredArgsConstructor
public class UiUser {
    private final UserContext ctx;
    public boolean isAdmin()            { return ctx.isAdmin(); }
    public boolean hasRole(String role) { return ctx.hasRole(role); }
    public String name()                { return ctx.currentUsername(); }

    public boolean hasAnyRole(String... roles) {
        return Arrays.stream(roles).anyMatch(ctx::hasRole);
    }
}

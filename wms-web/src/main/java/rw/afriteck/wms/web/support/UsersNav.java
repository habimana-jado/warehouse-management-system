package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsersNav implements NavContributor {
    @Override public String module() { return "admin"; }
    @Override public int order() { return 10; }
    @Override public List<NavSectionDef> sections() {
        return List.of(new NavSectionDef("nav.section.access", List.of(
                NavItemDef.of("users", "zmdi zmdi-accounts", "/users", NavRoles.ADMIN_ONLY))));
    }
}


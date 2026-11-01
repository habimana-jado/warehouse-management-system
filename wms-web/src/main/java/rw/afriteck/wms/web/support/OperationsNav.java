package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OperationsNav implements NavContributor {
    @Override public String module() { return "operations"; }
    @Override public List<NavSectionDef> sections() {
        return List.of(new NavSectionDef(null, List.of(
                NavItemDef.of("my-tasks",     "zmdi zmdi-assignment-check", "/tasks/my",           NavRoles.WORKER),
                NavItemDef.of("tasks",        "zmdi zmdi-format-list-bulleted", "/tasks",          NavRoles.BOARD),
                NavItemDef.of("productivity", "zmdi zmdi-trending-up",      "/tasks/productivity", NavRoles.MANAGER))));
    }
}

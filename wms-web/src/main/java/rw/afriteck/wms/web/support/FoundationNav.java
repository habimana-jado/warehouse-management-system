package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FoundationNav implements NavContributor {
    @Override public String module() { return "admin"; }
    @Override public int order() { return 20; }
    @Override public List<NavSectionDef> sections() {
        return List.of(new NavSectionDef("nav.section.masterData", List.of(
                NavItemDef.of("warehouses", "zmdi zmdi-store",      "/warehouses", NavRoles.ADMIN_ONLY),
                NavItemDef.of("locations",  "zmdi zmdi-view-comfy", "/locations",  NavRoles.BOARD),
                NavItemDef.of("categories", "zmdi zmdi-labels",     "/categories", NavRoles.BOARD),
                NavItemDef.of("items",      "zmdi zmdi-label",      "/items",      NavRoles.BOARD),
                NavItemDef.of("suppliers",  "zmdi zmdi-truck",      "/suppliers",  NavRoles.BOARD),
                NavItemDef.of("customers",  "zmdi zmdi-account-box","/customers",  NavRoles.BOARD))));
    }
}

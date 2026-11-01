package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryNav implements NavContributor {
    @Override public String module() { return "inventory"; }
    @Override public List<NavSectionDef> sections() {
        return List.of(
                new NavSectionDef("nav.section.stock", List.of(
                        NavItemDef.of("stock",    "zmdi zmdi-storage",   "/stock",                      NavRoles.EVERYONE),
                        NavItemDef.of("movements","zmdi zmdi-swap",      "/stock/movements",            NavRoles.BOARD),
                        NavItemDef.of("reorder",  "zmdi zmdi-trending-down", "/reorder-policies",       NavRoles.BOARD),
                        NavItemDef.of("low-stock","zmdi zmdi-alert-triangle","/reorder-policies/low-stock", NavRoles.BOARD))),
                new NavSectionDef("nav.section.documents", List.of(
                        NavItemDef.of("adjustments",        "zmdi zmdi-edit",      "/adjustments",         NavRoles.BOARD),
                        NavItemDef.of("my-adjustments",     "zmdi zmdi-edit",      "/adjustments/my",      NavRoles.WORKER),
                        NavItemDef.of("transfers",          "zmdi zmdi-swap-alt",  "/transfers",           NavRoles.BOARD),
                        NavItemDef.of("incoming-transfers", "zmdi zmdi-arrow-in",  "/transfers/incoming",  NavRoles.WORKER),
                        NavItemDef.of("new-transfer",       "zmdi zmdi-arrow-split","/transfers/new",      NavRoles.WORKER),
                        NavItemDef.of("counts",             "zmdi zmdi-check-all", "/counts",              NavRoles.BOARD),
                        NavItemDef.of("reasons",            "zmdi zmdi-label",     "/adjustment-reasons",  NavRoles.ADMIN_ONLY))));
    }
}

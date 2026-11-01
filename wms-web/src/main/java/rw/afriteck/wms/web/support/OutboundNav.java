package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboundNav implements NavContributor {
    @Override public String module() { return "outbound"; }
    @Override public List<NavSectionDef> sections() {
        return List.of(new NavSectionDef(null, List.of(
                NavItemDef.of("sales-orders",     "zmdi zmdi-shopping-cart", "/sales-orders",     NavRoles.BOARD),
                NavItemDef.of("pick-lists",       "zmdi zmdi-format-list-numbered", "/pick-lists", NavRoles.BOARD),
                NavItemDef.of("shipments",        "zmdi zmdi-truck",         "/shipments",        NavRoles.BOARD),
                NavItemDef.of("supplier-returns", "zmdi zmdi-undo",          "/supplier-returns", NavRoles.BOARD))));
    }
}

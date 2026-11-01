package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InboundNav implements NavContributor {
    @Override public String module() { return "inbound"; }
    @Override public List<NavSectionDef> sections() {
        return List.of(new NavSectionDef(null, List.of(
                NavItemDef.of("purchase-orders",  "zmdi zmdi-assignment",       "/purchase-orders",                 NavRoles.INBOUND_READ),
                NavItemDef.of("goods-receipts",   "zmdi zmdi-inbox",            "/goods-receipts",                  NavRoles.INBOUND_READ),
                NavItemDef.of("putaway-board",    "zmdi zmdi-playlist-plus",    "/goods-receipts/awaiting-putaway", NavRoles.INBOUND_READ),
                NavItemDef.of("customer-returns", "zmdi zmdi-undo",             "/customer-returns",                NavRoles.INBOUND_READ))));
    }
}

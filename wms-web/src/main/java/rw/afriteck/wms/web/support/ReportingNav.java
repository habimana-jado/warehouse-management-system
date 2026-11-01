package rw.afriteck.wms.web.support;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportingNav implements NavContributor {
    @Override public String module() { return "reporting"; }

    @Override public List<NavSectionDef> sections() {
        return List.of(
                new NavSectionDef("nav.section.reportsStock", List.of(
                        NavItemDef.of("report-stock",     "zmdi zmdi-storage",  "/reports/stock",     NavRoles.BOARD),
                        NavItemDef.of("report-expiry",    "zmdi zmdi-time",     "/reports/expiry",    NavRoles.BOARD),
                        NavItemDef.of("report-valuation", "zmdi zmdi-money",    "/reports/valuation", NavRoles.BOARD))),
                new NavSectionDef("nav.section.reportsInbound", List.of(
                        NavItemDef.of("report-open-orders",         "zmdi zmdi-assignment", "/reports/inbound/open-orders",         NavRoles.BOARD),
                        NavItemDef.of("report-supplier-performance","zmdi zmdi-thumb-up",   "/reports/inbound/supplier-performance",NavRoles.BOARD),
                        NavItemDef.of("report-putaway-backlog",     "zmdi zmdi-inbox",      "/reports/inbound/putaway-backlog",     NavRoles.BOARD))),
                new NavSectionDef("nav.section.reportsOutbound", List.of(
                        NavItemDef.of("report-fulfillment", "zmdi zmdi-shopping-cart", "/reports/outbound/fulfillment", NavRoles.BOARD),
                        NavItemDef.of("report-picking",     "zmdi zmdi-format-list-numbered", "/reports/outbound/picking", NavRoles.BOARD),
                        NavItemDef.of("report-shipments",   "zmdi zmdi-truck",         "/reports/outbound/shipments",   NavRoles.BOARD),
                        NavItemDef.of("report-open-demand", "zmdi zmdi-trending-down", "/reports/outbound/open-demand", NavRoles.BOARD))),
                new NavSectionDef("nav.section.reportsAccuracy", List.of(
                        NavItemDef.of("report-counts",      "zmdi zmdi-check-all", "/reports/accuracy/counts",      NavRoles.BOARD),
                        NavItemDef.of("report-adjustments", "zmdi zmdi-edit",      "/reports/accuracy/adjustments", NavRoles.BOARD),
                        NavItemDef.of("report-audit",       "zmdi zmdi-shield-check", "/reports/audit",             NavRoles.MANAGER))));
    }
}

package rw.afriteck.wms.web.reporting;

import java.util.List;

/**
 * Registry of the report screens shown on /reports. Each UI slice appends its groups here.
 */
public final class ReportCatalog {

    private ReportCatalog() {
    }

    public record Entry(String key, String path, boolean managerOnly) {
    }   // key: message key, "<key>.desc" is the description

    public record Group(String key, List<Entry> entries) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("reporting.group.stock", List.of(
                    new Entry("reporting.stock", "/reports/stock", false),
                    new Entry("reporting.expiry", "/reports/expiry", false),
                    new Entry("reporting.valuation", "/reports/valuation", false),
                    new Entry("reporting.lowstock", "/reorder-policies/low-stock", false),
                    new Entry("reporting.movements", "/stock/movements", false))),

            new Group("reporting.group.inbound", List.of(
                    new Entry("reporting.openPo", "/reports/inbound/open-orders", false),
                    new Entry("reporting.supplierPerf", "/reports/inbound/supplier-performance", false),
                    new Entry("reporting.putawayBacklog", "/reports/inbound/putaway-backlog", false))),

            new Group("reporting.group.outbound", List.of(
                    new Entry("reporting.fulfillment", "/reports/outbound/fulfillment", false),
                    new Entry("reporting.picking", "/reports/outbound/picking", false),
                    new Entry("reporting.shipmentsDaily", "/reports/outbound/shipments", false),
                    new Entry("reporting.openDemand", "/reports/outbound/open-demand", false))),

            new Group("reporting.group.accuracy", List.of(
                    new Entry("reporting.countAccuracy", "/reports/accuracy/counts", false),
                    new Entry("reporting.adjustmentsReport", "/reports/accuracy/adjustments", false))),
            new Group("reporting.group.tasks", List.of(
                    new Entry("reporting.taskBacklog", "/reports/tasks/backlog", false),
                    new Entry("reporting.productivity", "/tasks/productivity", true))),
            new Group("reporting.group.audit", List.of(
                    new Entry("reporting.audit", "/reports/audit", true)))

    );

    public static List<Group> visible(boolean manager) {
        return GROUPS.stream()
                .map(g -> new Group(g.key(), g.entries().stream().filter(e -> manager || !e.managerOnly()).toList()))
                .filter(g -> !g.entries().isEmpty())
                .toList();
    }
}

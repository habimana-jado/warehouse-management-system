package rw.afriteck.wms.reporting.controller;

final class ReportingAccess {
    private ReportingAccess() {}
    static final String BOARD = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')";
    static final String MANAGER = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER')";
}

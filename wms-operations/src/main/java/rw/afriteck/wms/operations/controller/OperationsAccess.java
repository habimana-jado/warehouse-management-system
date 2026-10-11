package rw.afriteck.wms.operations.controller;

final class OperationsAccess {
    private OperationsAccess() {}

    static final String MANAGER  = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER')";
    static final String BOARD    = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')";
    static final String WORKER   = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','RECEIVER','PICKER')";
    static final String ANY_USER = "isAuthenticated()";
}

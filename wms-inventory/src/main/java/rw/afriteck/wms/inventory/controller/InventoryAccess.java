package rw.afriteck.wms.inventory.controller;

final class InventoryAccess {
    private InventoryAccess() {}

    static final String ADMIN    = "hasRole('ADMIN')";
    static final String MANAGER  = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER')";
    static final String BOARD    = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')";
    static final String WORKER   = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','RECEIVER','PICKER')";
    static final String ANY_USER = "isAuthenticated()";
}

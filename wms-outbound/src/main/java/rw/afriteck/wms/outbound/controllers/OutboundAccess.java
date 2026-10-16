package rw.afriteck.wms.outbound.controllers;

import rw.afriteck.wms.common.security.UserContext;

final class OutboundAccess {
    private OutboundAccess() {}
    static final String MANAGER   = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER')";
    static final String READ      = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')";
    static final String PICK      = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','PICKER')";
    static final String PICK_READ = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER','PICKER')";
}

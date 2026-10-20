package rw.afriteck.wms.web.config;

public final class WebAccess {
    private WebAccess() {}

    public static final String ADMIN    = "hasRole('ADMIN')";
    public static final String MANAGER  = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER')";
    public static final String BOARD    = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','VIEWER')";
    public static final String WORKER   = "hasAnyRole('ADMIN','WAREHOUSE_MANAGER','RECEIVER','PICKER')";
    public static final String ANY_USER = "isAuthenticated()";
}

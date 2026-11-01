package rw.afriteck.wms.web.support;

import java.util.List;

public final class NavRoles {
    private NavRoles() {}
    public static final List<String> ADMIN_ONLY   = List.of("ADMIN");
    public static final List<String> MANAGER      = List.of("ADMIN", "WAREHOUSE_MANAGER");
    public static final List<String> BOARD        = List.of("ADMIN", "WAREHOUSE_MANAGER", "VIEWER");
    public static final List<String> WORKER       = List.of("ADMIN", "WAREHOUSE_MANAGER", "RECEIVER", "PICKER");
    public static final List<String> INBOUND_READ = List.of("ADMIN", "WAREHOUSE_MANAGER", "VIEWER", "RECEIVER");
    public static final List<String> EVERYONE     = List.of("ADMIN", "WAREHOUSE_MANAGER", "VIEWER", "RECEIVER", "PICKER");
}


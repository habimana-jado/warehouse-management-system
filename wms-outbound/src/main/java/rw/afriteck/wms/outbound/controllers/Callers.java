package rw.afriteck.wms.outbound.controllers;

import rw.afriteck.wms.common.security.UserContext;

final class Callers {
    private Callers() {}
    /** Admin or WAREHOUSE_MANAGER: may act on other people's work. */
    static boolean privileged(UserContext u) { return u.isAdmin() || u.hasRole("WAREHOUSE_MANAGER"); }
    static boolean viewer(UserContext u)     { return u.hasRole("VIEWER"); }
}


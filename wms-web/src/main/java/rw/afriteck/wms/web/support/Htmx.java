package rw.afriteck.wms.web.support;

import jakarta.servlet.http.HttpServletRequest;

public final class Htmx {
    private Htmx() {}
    public static boolean isRequest(HttpServletRequest r) { return "true".equals(r.getHeader("HX-Request")); }
    public static boolean isFragmentRequest(HttpServletRequest r) {
        return isRequest(r) && !"true".equals(r.getHeader("HX-Boosted"));
    }
}

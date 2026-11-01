package rw.afriteck.wms.web.support;

import java.util.List;

public record NavItemDef(String key, String icon, String url, List<String> roles) {
    public static NavItemDef of(String key, String icon, String url, List<String> roles) {
        return new NavItemDef(key, icon, url, roles);
    }
}

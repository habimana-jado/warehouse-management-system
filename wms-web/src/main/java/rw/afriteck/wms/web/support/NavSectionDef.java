package rw.afriteck.wms.web.support;

import java.util.List;

public record NavSectionDef(String titleKey, List<NavItemDef> items) {}   // titleKey may be null

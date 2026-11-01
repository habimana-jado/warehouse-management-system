package rw.afriteck.wms.web.support;

import java.util.List;

public record NavView(List<NavModuleView> modules, NavModuleView module, List<NavSectionView> sections) {}

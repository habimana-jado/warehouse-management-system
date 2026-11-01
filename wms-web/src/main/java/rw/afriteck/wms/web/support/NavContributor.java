package rw.afriteck.wms.web.support;

import java.util.List; /** One per feature chat. Several contributors can add sections to the same module. */
public interface NavContributor {
    String module();                         // inbound, outbound, inventory, operations, reporting, admin
    List<NavSectionDef> sections();
    default int order() { return 100; }
}

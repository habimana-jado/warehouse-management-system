package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReasonFilterForm {
    private String q;
    private boolean includeInactive;
}

package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderFilterForm {
    private boolean activeOnly = true;       // the first visit shows active policies; unticking sends the "_activeOnly" marker
}

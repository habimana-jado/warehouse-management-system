package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.inventory.enums.CountStatus;

@Getter
@Setter
public class CountFilterForm {
    private String q;
    private CountStatus status;
}


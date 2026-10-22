package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;

@Getter
@Setter
public class AdjustmentFilterForm {
    private String q;
    private AdjustmentStatus status;                              // blank = all
}

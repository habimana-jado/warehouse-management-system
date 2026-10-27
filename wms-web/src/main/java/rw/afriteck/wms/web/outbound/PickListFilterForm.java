package rw.afriteck.wms.web.outbound;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class PickListFilterForm {
    private String status;                                         // blank = open lists, ALL, or one status
}

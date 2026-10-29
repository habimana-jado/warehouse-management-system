package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenOrdersFilterForm {
    private boolean overdueOnly = false;
    private int page = 0;
    private int size = 15;
}


package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenDemandFilterForm {
    private boolean shortfallOnly = false;
    private int page = 0;
    private int size = 15;
}

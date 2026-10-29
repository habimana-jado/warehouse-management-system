package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ExpiryFilterForm {
    private Integer days;                  // blank = the configured default (30)
    private boolean includeExpired = true;
    private int page = 0;
    private int size = 15;
}

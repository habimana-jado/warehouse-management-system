package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportSearchForm {            // stock summary and valuation: search text + paging
    private String q;
    private int page = 0;
    private int size = 15;
}


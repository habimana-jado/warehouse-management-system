package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportPageForm {                 // paged reports without filters (put-away backlog, later task backlog)
    private int page = 0;
    private int size = 15;
}

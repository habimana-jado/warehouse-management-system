package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopItemsFilterForm extends DateRangeFilterForm {
    private int topItems = 10;
}


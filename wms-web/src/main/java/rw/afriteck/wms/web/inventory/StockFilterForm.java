package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StockFilterForm {
    private UUID locationId;
    private String item;
    private String serial;
    private boolean includeZero;
}

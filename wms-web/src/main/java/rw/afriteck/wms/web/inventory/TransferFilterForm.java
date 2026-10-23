package rw.afriteck.wms.web.inventory;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.inventory.enums.TransferStatus;

@Getter
@Setter
public class TransferFilterForm {
    private String q;
    private TransferStatus status;
}

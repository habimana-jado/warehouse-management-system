package rw.afriteck.wms.web.outbound;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;

import java.math.BigDecimal;

@Getter
@Setter
class SalesOrderLineForm {
    private String item;                                           // SKU, name or barcode (ItemResolver)
    private BigDecimal quantity;
    @Size(max = 30)
    private String uom;                                            // empty = base unit
    private BigDecimal unitPrice;                                  // per the unit above; empty = unknown
    @Size(max = 255)
    private String note;

    boolean blank() {
        return Codes.blankToNull(item) == null && quantity == null && unitPrice == null
                && Codes.blankToNull(uom) == null && Codes.blankToNull(note) == null;
    }
}

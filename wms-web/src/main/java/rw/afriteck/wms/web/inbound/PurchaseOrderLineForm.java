package rw.afriteck.wms.web.inbound;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;

import java.math.BigDecimal;
import java.util.UUID;

/** One row of the purchase order form. `item` is what the user typed (SKU, name or barcode). */
@Getter
@Setter
public class PurchaseOrderLineForm {

    private UUID lineId;                       // set when editing an existing line

    private String item;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;

    @Size(max = 30)
    private String uom;                        // empty = the item's base unit

    @DecimalMin("0")
    private BigDecimal unitPrice;              // per uom

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal tolerancePercent;

    @Size(max = 500)
    private String notes;

    /** A row nobody touched: it is skipped instead of being reported as an error. */
    boolean blank() {
        return lineId == null
                && Codes.blankToNull(item) == null
                && quantity == null
                && Codes.blankToNull(uom) == null
                && unitPrice == null
                && tolerancePercent == null
                && Codes.blankToNull(notes) == null;
    }
}
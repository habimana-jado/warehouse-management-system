package rw.afriteck.wms.web.inbound;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the create form. `item` is what the user typed (SKU, name or barcode). */
@Getter
@Setter
public class ReturnLineForm {

    private String item;

    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;

    @Size(max = 30)
    private String uom;                       // empty = the item's base unit

    @Size(max = 100)
    private String lotNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @Size(max = 100)
    private String serialNumber;

    @Size(max = 500)
    private String returnReason;

    boolean blank() {
        return Codes.blankToNull(item) == null && quantity == null && Codes.blankToNull(uom) == null
                && Codes.blankToNull(lotNumber) == null && expiryDate == null
                && Codes.blankToNull(serialNumber) == null && Codes.blankToNull(returnReason) == null;
    }
}
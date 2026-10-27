package rw.afriteck.wms.web.inbound;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the receive form. A row with no accepted and no rejected quantity is ignored. */
@Getter
@Setter
public class ReceiveLineForm {

    private UUID purchaseOrderLineId;

    @Size(max = 30)
    private String uom;                       // empty = the item's base unit

    @DecimalMin("0")
    private BigDecimal acceptedQty;

    @DecimalMin("0")
    private BigDecimal rejectedQty;

    @Size(max = 500)
    private String rejectReason;

    @Size(max = 100)
    private String lotNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @Size(max = 100)
    private String serialNumber;

    private UUID locationId;                  // storage bin, direct mode only (empty = suggested)

    boolean blank() {
        return isZero(acceptedQty) && isZero(rejectedQty);
    }

    private static boolean isZero(BigDecimal v) {
        return v == null || v.signum() == 0;
    }
}
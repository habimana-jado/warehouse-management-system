package rw.afriteck.wms.web.inbound;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.inbound.enums.ReturnDisposition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the receive form. A row with no quantity counts as "not returned" and is not sent. */
@Getter
@Setter
public class ReceiveReturnLineForm {

    private UUID lineId;

    @DecimalMin("0")
    private BigDecimal receivedQty;

    private ReturnDisposition disposition;

    private UUID locationId;                  // empty = automatic bin

    @Size(max = 100)
    private String lotNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @Size(max = 100)
    private String serialNumber;

    boolean blank() {
        return receivedQty == null || receivedQty.signum() == 0;
    }
}
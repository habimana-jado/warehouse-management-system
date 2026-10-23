package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ReceiveLineForm {

    private UUID lineId;
    private UUID toLocationId;
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) private BigDecimal quantity;   // blank = nothing received for this line

    public ReceiveLineForm() {}

    ReceiveLineForm(UUID lineId, UUID toLocationId, BigDecimal quantity) {
        this.lineId = lineId;
        this.toLocationId = toLocationId;
        this.quantity = quantity;
    }
}

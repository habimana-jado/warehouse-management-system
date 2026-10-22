package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.AdjustmentLineRequest;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AdjustmentLineForm {

    @NotNull
    private UUID locationId;
    @NotBlank
    @Size(max = 100)
    private String item;
    @NotNull
    private AdjustmentDirection direction;               // INCREASE or DECREASE (the form never offers BOTH)
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 15, fraction = 4)
    private BigDecimal quantity;
    @Size(max = 64)
    private String lotNumber;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;
    @Size(max = 64)
    private String serialNumber;

    /**
     * The service expects a signed delta.
     */
    AdjustmentLineRequest toRequest(UUID itemId) {
        BigDecimal signed = direction == AdjustmentDirection.DECREASE ? quantity.negate() : quantity;
        return new AdjustmentLineRequest(locationId, itemId, Codes.blankToNull(lotNumber), expiryDate,
                Codes.blankToNull(serialNumber), signed);
    }
}

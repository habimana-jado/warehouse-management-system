package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.AddCountLineRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class AddCountLineForm {
    @NotBlank
    @Size(max = 100) private String item;           // SKU, name or barcode
    @NotNull
    private UUID locationId;
    @Size(max = 64) private String lotNumber;                   // an EXISTING lot for lot items
    @Size(max = 64) private String serialNumber;
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) private BigDecimal countedQty;

    AddCountLineRequest toRequest(UUID itemId) {
        return new AddCountLineRequest(locationId, itemId, Codes.blankToNull(lotNumber),
                Codes.blankToNull(serialNumber), countedQty);
    }
}

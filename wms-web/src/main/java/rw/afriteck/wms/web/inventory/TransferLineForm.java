package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.CreateTransferLineRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TransferLineForm {

    @NotBlank
    @Size(max = 100) private String item;           // SKU, name or barcode
    @Size(max = 64) private String lotNumber;                   // LOT items: an existing lot
    @Size(max = 64) private String serialNumber;                // SERIAL items
    @NotNull
    private UUID fromLocationId;
    private UUID toLocationId;                                  // bin transfers; checked in the controller (depends on the mode)
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) private BigDecimal quantity;

    CreateTransferLineRequest toRequest(UUID itemId, boolean inter) {
        return new CreateTransferLineRequest(itemId, Codes.blankToNull(lotNumber), Codes.blankToNull(serialNumber),
                fromLocationId, inter ? null : toLocationId, quantity);
    }
}

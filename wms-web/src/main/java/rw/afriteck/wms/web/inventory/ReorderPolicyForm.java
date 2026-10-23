package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.inventory.dtos.CreateReorderPolicyRequest;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ReorderPolicyForm {

    @NotBlank
    @Size(max = 100) private String item;     // create: SKU, name or barcode. Edit: the SKU (hidden, ignored: the item is immutable)
    @NotNull
    @DecimalMin("0") @Digits(integer = 15, fraction = 4) private BigDecimal minQty;
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) private BigDecimal reorderQty;
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 15, fraction = 4) private BigDecimal maxQty;   // optional
    private boolean active = true;                       // edit only

    static ReorderPolicyForm from(ReorderPolicyResponse p) {
        ReorderPolicyForm f = new ReorderPolicyForm();
        f.setItem(p.itemSku());
        f.setMinQty(p.minQty());
        f.setReorderQty(p.reorderQty());
        f.setMaxQty(p.maxQty());
        f.setActive(p.active());
        return f;
    }

    CreateReorderPolicyRequest toCreateRequest(UUID warehouseId, UUID itemId) {
        return new CreateReorderPolicyRequest(warehouseId, itemId, minQty, reorderQty, maxQty);
    }

    /** A blank maximum on the edit form means "no maximum". */
    UpdateReorderPolicyRequest toUpdateRequest() {
        return new UpdateReorderPolicyRequest(minQty, reorderQty, maxQty, maxQty == null, active);
    }
}

package rw.afriteck.wms.web.foundation.items;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.ItemResponseDetailed;
import rw.afriteck.wms.foundation.dtos.UpdateItemRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class ItemEditForm {
    @NotBlank @Size(max = 200)
    private String name;
    private String description;
    @Size(max = 60)
    private String barcode;
    private UUID categoryId;
    @PositiveOrZero
    private BigDecimal unitCost;
    @PositiveOrZero
    private BigDecimal weightKg;

    public static ItemEditForm from(ItemResponseDetailed d) {
        ItemEditForm f = new ItemEditForm();
        f.name = d.name();
        f.description = d.description();
        f.barcode = d.barcode();
        f.categoryId = d.categoryId();
        f.unitCost = d.unitCost();
        f.weightKg = d.weightKg();
        return f;
    }

    public UpdateItemRequest toRequest() {
        return new UpdateItemRequest(name, Codes.blankToNull(description), Codes.blankToNull(barcode),
                categoryId, unitCost, weightKg);
    }
}

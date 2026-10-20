package rw.afriteck.wms.web.foundation.items;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.*;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemUomForm {
    @Size(max = 10)
    private String uom;
    @Positive
    private BigDecimal factorToBase;
    @Size(max = 60)
    private String barcode;

    /** A row the user left empty: ignored on save. */
    public boolean blank() {
        return !StringUtils.hasText(uom) && factorToBase == null && !StringUtils.hasText(barcode);
    }

    public static ItemUomForm from(ItemUomResponse u) {
        ItemUomForm f = new ItemUomForm();
        f.uom = u.uom();
        f.factorToBase = u.factorToBase();
        f.barcode = u.barcode();
        return f;
    }

    public ItemUomRequest toRequest() {
        return new ItemUomRequest(uom, factorToBase, Codes.blankToNull(barcode));
    }
}


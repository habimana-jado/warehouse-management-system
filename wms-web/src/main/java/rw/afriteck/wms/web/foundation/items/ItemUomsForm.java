package rw.afriteck.wms.web.foundation.items;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.foundation.dtos.ItemResponseDetailed;
import rw.afriteck.wms.foundation.dtos.ReplaceItemUomsRequest;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ItemUomsForm {
    @Valid
    private List<ItemUomForm> uoms = new ArrayList<>();

    public static ItemUomsForm from(ItemResponseDetailed d) {
        ItemUomsForm f = new ItemUomsForm();
        d.uoms().forEach(u -> f.uoms.add(ItemUomForm.from(u)));
        for (int i = 0; i < 2; i++) f.uoms.add(new ItemUomForm());          // two blank rows to add units
        return f;
    }

    public ReplaceItemUomsRequest toRequest() {
        return new ReplaceItemUomsRequest(uoms.stream().filter(u -> !u.blank()).map(ItemUomForm::toRequest).toList());
    }
}

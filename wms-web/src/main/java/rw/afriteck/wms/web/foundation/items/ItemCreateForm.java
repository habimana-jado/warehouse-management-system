package rw.afriteck.wms.web.foundation.items;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.CreateItemRequest;
import rw.afriteck.wms.foundation.dtos.ItemUomRequest;
import rw.afriteck.wms.foundation.enums.TrackingType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter
public class ItemCreateForm {
    @NotBlank
    @Size(max = 40)
    private String sku;
    @NotBlank @Size(max = 200)
    private String name;
    private String description;
    @Size(max = 60)
    private String barcode;
    private UUID categoryId;
    @NotBlank @Size(max = 10)
    private String baseUom;
    @NotNull
    private TrackingType trackingType = TrackingType.NONE;
    private boolean expiryTracked;
    @PositiveOrZero
    private BigDecimal unitCost;
    @PositiveOrZero
    private BigDecimal weightKg;
    @Valid
    private List<ItemUomForm> uoms = new ArrayList<>();

    public ItemCreateForm() {
        for (int i = 0; i < 3; i++) uoms.add(new ItemUomForm());           // three empty unit rows
    }

    public CreateItemRequest toRequest() {
        List<ItemUomRequest> rows = uoms.stream().filter(u -> !u.blank()).map(ItemUomForm::toRequest).toList();
        return new CreateItemRequest(sku, name, Codes.blankToNull(description), Codes.blankToNull(barcode),
                categoryId, baseUom, trackingType, expiryTracked, unitCost, weightKg, rows);
    }
}

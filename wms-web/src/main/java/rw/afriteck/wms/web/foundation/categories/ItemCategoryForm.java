package rw.afriteck.wms.web.foundation.categories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.CreateItemCategoryRequest;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.dtos.UpdateItemCategoryRequest;

@Getter
@Setter
public class ItemCategoryForm {
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 255)
    private String description;

    public static ItemCategoryForm from(ItemCategoryResponse c) {
        ItemCategoryForm f = new ItemCategoryForm();
        f.name = c.name();
        f.description = c.description();
        return f;
    }

    public CreateItemCategoryRequest toCreateRequest() {
        return new CreateItemCategoryRequest(name, Codes.blankToNull(description));
    }

    public UpdateItemCategoryRequest toUpdateRequest() {
        return new UpdateItemCategoryRequest(name, Codes.blankToNull(description));
    }
}
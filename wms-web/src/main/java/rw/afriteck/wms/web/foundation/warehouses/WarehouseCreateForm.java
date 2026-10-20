package rw.afriteck.wms.web.foundation.warehouses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.CreateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.UpdateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;

@Getter
@Setter
public class WarehouseCreateForm {
    @NotBlank
    @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9._-]+")
    private String code;
    @NotBlank @Size(max = 120)
    private String name;
    @Size(max = 255)
    private String addressLine;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String country;

    public CreateWarehouseRequest toRequest() {
        return new CreateWarehouseRequest(code, name,
                Codes.blankToNull(addressLine), Codes.blankToNull(city), Codes.blankToNull(country));
    }
}


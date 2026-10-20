package rw.afriteck.wms.web.foundation.warehouses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.UpdateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;

@Getter @Setter
public class WarehouseEditForm {
    @NotBlank @Size(max = 120)
    private String name;
    @Size(max = 255)
    private String addressLine;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String country;

    public static WarehouseEditForm from(WarehouseResponse w) {
        WarehouseEditForm f = new WarehouseEditForm();
        f.name = w.name();
        f.addressLine = w.addressLine();
        f.city = w.city();
        f.country = w.country();
        return f;
    }

    public UpdateWarehouseRequest toRequest() {
        return new UpdateWarehouseRequest(name,
                Codes.blankToNull(addressLine), Codes.blankToNull(city), Codes.blankToNull(country));
    }
}

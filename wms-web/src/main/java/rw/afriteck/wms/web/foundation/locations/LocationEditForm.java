package rw.afriteck.wms.web.foundation.locations;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.UpdateLocationRequest;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;

@Getter @Setter
public class LocationEditForm {
    @Size(max = 120)
    private String name;
    private LocationUsage usage;                            // null when the field is disabled (non-bin): the service keeps the current one
    @Size(max = 60)
    private String barcode;
    @PositiveOrZero
    private BigDecimal maxCapacity;

    public static LocationEditForm from(LocationResponse l) {
        LocationEditForm f = new LocationEditForm();
        f.name = l.name();
        f.usage = l.usage();
        f.barcode = l.barcode();
        f.maxCapacity = l.maxCapacity();
        return f;
    }

    public UpdateLocationRequest toRequest() {
        return new UpdateLocationRequest(Codes.blankToNull(name), usage, Codes.blankToNull(barcode), maxCapacity);
    }
}

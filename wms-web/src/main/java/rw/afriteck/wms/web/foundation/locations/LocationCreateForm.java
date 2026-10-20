package rw.afriteck.wms.web.foundation.locations;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.CreateLocationRequest;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class LocationCreateForm {
    private UUID parentId;                                  // empty = top level
    @NotNull
    private LocationType type = LocationType.ZONE;
    @NotBlank
    @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9._-]+")
    private String code;
    @Size(max = 120)
    private String name;
    @NotNull
    private LocationUsage usage = LocationUsage.STORAGE;
    @Size(max = 60)
    private String barcode;
    @PositiveOrZero
    private BigDecimal maxCapacity;

    public CreateLocationRequest toRequest(UUID warehouseId) {
        return new CreateLocationRequest(warehouseId, parentId, code, Codes.blankToNull(name),
                type, usage, Codes.blankToNull(barcode), maxCapacity);
    }
}


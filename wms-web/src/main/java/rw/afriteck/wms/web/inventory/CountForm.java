package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.CreateCountRequest;

import java.util.UUID;

@Getter @Setter
public class CountForm {
    private UUID zoneLocationId;                       // blank = the whole warehouse
    @Size(max = 500) private String notes;

    CreateCountRequest toRequest(UUID warehouseId) {
        return new CreateCountRequest(warehouseId, zoneLocationId, Codes.blankToNull(notes));
    }
}

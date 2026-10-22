package rw.afriteck.wms.web.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.AdjustmentLineRequest;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AdjustmentForm {

    @NotNull
    private UUID reasonId;
    @Size(max = 500) private String notes;
    @Valid
    @NotEmpty
    private List<AdjustmentLineForm> lines = new ArrayList<>();

    CreateAdjustmentRequest toRequest(UUID warehouseId, List<UUID> itemIds) {
        List<AdjustmentLineRequest> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            out.add(lines.get(i).toRequest(itemIds.get(i)));
        }
        return new CreateAdjustmentRequest(warehouseId, reasonId, Codes.blankToNull(notes), out);
    }
}

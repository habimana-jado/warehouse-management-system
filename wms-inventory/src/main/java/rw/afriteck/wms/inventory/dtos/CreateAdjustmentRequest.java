package rw.afriteck.wms.inventory.dtos;

import java.util.List;
import java.util.UUID;

public record CreateAdjustmentRequest(
        UUID warehouseId, UUID reasonId, String notes,
        List<AdjustmentLineRequest> lines) {
}

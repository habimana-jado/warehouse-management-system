package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;

public final class AdjustmentReasonMapper {
    private AdjustmentReasonMapper() {}
    public static AdjustmentReasonResponse toResponse(AdjustmentReason r) {
        return new AdjustmentReasonResponse(r.getId(), r.getVersion(), r.getCode(), r.getName(),
                r.getDirection(), r.isRequiresApproval(), r.isActive());
    }
}

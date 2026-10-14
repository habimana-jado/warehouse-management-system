package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.entity.ReorderPolicy;

public final class ReorderPolicyMapper {
    private ReorderPolicyMapper() {}

    public static ReorderPolicyResponse toResponse(ReorderPolicy p, ItemResponse item) {
        return new ReorderPolicyResponse(p.getId(), p.getVersion(), p.getWarehouseId(),
                p.getItemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                item == null ? null : item.baseUom(),
                p.getMinQty(), p.getReorderQty(), p.getMaxQty(), p.isActive());
    }
}

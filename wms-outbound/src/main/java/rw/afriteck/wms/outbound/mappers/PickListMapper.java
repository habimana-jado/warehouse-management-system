package rw.afriteck.wms.outbound.mappers;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.outbound.dtos.PickAllocationResponse;
import rw.afriteck.wms.outbound.dtos.PickLineResponse;
import rw.afriteck.wms.outbound.dtos.PickListResponse;
import rw.afriteck.wms.outbound.dtos.PickListSummaryResponse;
import rw.afriteck.wms.outbound.entity.PickAllocation;
import rw.afriteck.wms.outbound.entity.PickLine;
import rw.afriteck.wms.outbound.entity.PickList;
import rw.afriteck.wms.outbound.entity.SalesOrder;

import java.util.Map;
import java.util.UUID;

public final class PickListMapper {
    private PickListMapper() {}

    public static PickListResponse toResponse(PickList p, String customerName,
                                              Map<UUID, ItemResponse> items, Map<UUID, LocationResponse> locations) {
        SalesOrder o = p.getSalesOrder();
        return new PickListResponse(p.getId(), p.getVersion(), p.getPickNumber(),
                o.getId(), o.getOrderNumber(), o.getCustomerId(), customerName, p.getWarehouseId(),
                p.getStatus(), p.getTaskId(), p.getReleasedAt(), p.getCompletedAt(), p.getCancelReason(),
                p.getLines().stream().map(l -> toLineResponse(l, items.get(l.getItemId()), locations)).toList());
    }

    public static PickLineResponse toLineResponse(PickLine l, ItemResponse item, Map<UUID, LocationResponse> locations) {
        return new PickLineResponse(l.getId(), l.getLineNo(), l.getSalesOrderLine().getId(),
                l.getItemId(), MapperSupport.sku(item), MapperSupport.name(item), MapperSupport.uom(item),
                MapperSupport.tracking(item), MapperSupport.expiryTracked(item),
                l.getPlannedQty(), l.getPickedQty(), l.getStatus(),
                l.getSuggestedLocationId(), MapperSupport.locationCode(locations, l.getSuggestedLocationId()),
                l.getSuggestedLotNumber(), l.getShortReason(),
                l.getAllocations().stream().map(a -> toAllocationResponse(a, locations)).toList());
    }

    public static PickAllocationResponse toAllocationResponse(PickAllocation a, Map<UUID, LocationResponse> locations) {
        return new PickAllocationResponse(a.getId(), a.getLocationId(),
                MapperSupport.locationCode(locations, a.getLocationId()),
                a.getLotNumber(), a.getSerialNumber(), a.getQuantity(), a.isExpiryOverride());
    }

    /** lineCount and doneLineCount come from the grouped count queries (done = total minus PENDING). */
    public static PickListSummaryResponse toSummary(PickList p, int lineCount, int doneLineCount) {
        SalesOrder o = p.getSalesOrder();      // join-fetched by PickListRepository.search
        return new PickListSummaryResponse(p.getId(), p.getPickNumber(), o.getId(), o.getOrderNumber(),
                p.getWarehouseId(), p.getStatus(), p.getTaskId(), p.getReleasedAt(), lineCount, doneLineCount);
    }
}

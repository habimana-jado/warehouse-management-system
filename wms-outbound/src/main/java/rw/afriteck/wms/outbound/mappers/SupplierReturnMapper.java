package rw.afriteck.wms.outbound.mappers;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.outbound.dtos.SupplierReturnLineResponse;
import rw.afriteck.wms.outbound.dtos.SupplierReturnResponse;
import rw.afriteck.wms.outbound.dtos.SupplierReturnSummaryResponse;
import rw.afriteck.wms.outbound.entity.SupplierReturn;
import rw.afriteck.wms.outbound.entity.SupplierReturnLine;

import java.util.Map;
import java.util.UUID;

public final class SupplierReturnMapper {
    private SupplierReturnMapper() {}

    public static SupplierReturnResponse toResponse(SupplierReturn r, String supplierName,
                                                    Map<UUID, ItemResponse> items, Map<UUID, LocationResponse> locations) {
        return new SupplierReturnResponse(r.getId(), r.getVersion(), r.getReturnNumber(),
                r.getSupplierId(), supplierName, r.getWarehouseId(), r.getStatus(),
                r.getReturnDate(), r.getReason(), r.getSupplierReference(), r.getNotes(), r.getCancelReason(),
                r.getPostedAt(), r.getCreatedAt(),
                r.getLines().stream().map(l -> toLineResponse(l, items.get(l.getItemId()), locations)).toList());
    }

    public static SupplierReturnLineResponse toLineResponse(SupplierReturnLine l, ItemResponse item,
                                                            Map<UUID, LocationResponse> locations) {
        return new SupplierReturnLineResponse(l.getId(), l.getLineNo(), l.getItemId(),
                MapperSupport.sku(item), MapperSupport.name(item), MapperSupport.uom(item),
                l.getLocationId(), MapperSupport.locationCode(locations, l.getLocationId()),
                l.getLotNumber(), l.getSerialNumber(), l.getQuantity(), l.getNote());
    }

    public static SupplierReturnSummaryResponse toSummary(SupplierReturn r, String supplierName, int lineCount) {
        return new SupplierReturnSummaryResponse(r.getId(), r.getReturnNumber(), r.getSupplierId(), supplierName,
                r.getWarehouseId(), r.getStatus(), r.getReturnDate(), r.getReason(), r.getSupplierReference(), lineCount);
    }
}

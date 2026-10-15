package rw.afriteck.wms.inbound.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inbound.dtos.CustomerReturnLineResponse;
import rw.afriteck.wms.inbound.dtos.CustomerReturnResponse;
import rw.afriteck.wms.inbound.dtos.CustomerReturnSummaryResponse;
import rw.afriteck.wms.inbound.entity.CustomerReturn;
import rw.afriteck.wms.inbound.entity.CustomerReturnLine;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CustomerReturnMapper {

    private CustomerReturnMapper() {}

    /** customerName may be null (walk-in return or unknown customer). */
    public static CustomerReturnResponse toResponse(CustomerReturn r, String customerName,
                                                    Map<UUID, ItemResponse> items,
                                                    Map<UUID, String> locationCodes,
                                                    Map<UUID, String> userNames) {
        List<CustomerReturnLineResponse> lines = r.getLines().stream()
                .map(l -> toLineResponse(l, items, locationCodes))
                .toList();
        return new CustomerReturnResponse(
                r.getId(), r.getVersion(), r.getReturnNumber(),
                r.getWarehouseId(),
                r.getCustomerId(), customerName,
                r.getSalesOrderId(), r.getSalesOrderNumber(),
                r.getStatus(), r.getNotes(),
                r.getReceivedAt(), r.getReceivedBy(),
                r.getReceivedBy() == null ? null : userNames.get(r.getReceivedBy()),
                r.getCancelledAt(), r.getCancelReason(),
                r.getCreatedAt(),
                lines);
    }

    public static CustomerReturnLineResponse toLineResponse(CustomerReturnLine l,
                                                            Map<UUID, ItemResponse> items,
                                                            Map<UUID, String> locationCodes) {
        ItemResponse item = items.get(l.getItemId());
        return new CustomerReturnLineResponse(
                l.getId(), l.getLineNo(),
                l.getItemId(),
                item == null ? null : item.sku(),
                item == null ? null : item.name(),
                item == null ? null : item.baseUom(),
                l.getExpectedQty(), l.getReceivedQty(),
                l.getLotNumber(), l.getExpiryDate(), l.getSerialNumber(),
                l.getReturnReason(),
                l.getDisposition(),
                l.getLocationId(),
                l.getLocationId() == null ? null : locationCodes.get(l.getLocationId()));
    }

    public static CustomerReturnSummaryResponse toSummary(CustomerReturn r, String customerName, int lineCount) {
        return new CustomerReturnSummaryResponse(
                r.getId(), r.getReturnNumber(),
                r.getWarehouseId(),
                r.getCustomerId(), customerName,
                r.getSalesOrderNumber(),
                r.getStatus(),
                r.getCreatedAt(), r.getReceivedAt(),
                lineCount);
    }
}
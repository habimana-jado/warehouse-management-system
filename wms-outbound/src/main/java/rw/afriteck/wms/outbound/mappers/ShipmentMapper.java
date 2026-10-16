package rw.afriteck.wms.outbound.mappers;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentLineResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentSummaryResponse;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.Shipment;
import rw.afriteck.wms.outbound.entity.ShipmentLine;

import java.util.Map;
import java.util.UUID;

public final class ShipmentMapper {
    private ShipmentMapper() {}

    public static ShipmentResponse toResponse(Shipment s, String customerName, Map<UUID, ItemResponse> items) {
        SalesOrder o = s.getSalesOrder();
        return new ShipmentResponse(s.getId(), s.getVersion(), s.getShipmentNumber(),
                o.getId(), o.getOrderNumber(), s.getCustomerId(), customerName, s.getWarehouseId(),
                s.getStatus(), s.getShippedAt(), s.getShipToAddress(), s.getCarrier(), s.getTrackingReference(),
                s.getPackageCount(), s.getNotes(), s.getDeliveredAt(), s.getReceivedByName(), s.getPodNotes(),
                s.getCreatedAt(),
                s.getLines().stream().map(l -> toLineResponse(l, items.get(l.getItemId()))).toList());
    }

    public static ShipmentLineResponse toLineResponse(ShipmentLine l, ItemResponse item) {
        return new ShipmentLineResponse(l.getId(), l.getLineNo(), l.getSalesOrderLine().getId(), l.getItemId(),
                MapperSupport.sku(item), MapperSupport.name(item), MapperSupport.uom(item), l.getQuantity());
    }

    public static ShipmentSummaryResponse toSummary(Shipment s, String customerName, int lineCount) {
        SalesOrder o = s.getSalesOrder();      // join-fetched by ShipmentRepository.search
        return new ShipmentSummaryResponse(s.getId(), s.getShipmentNumber(), o.getId(), o.getOrderNumber(),
                s.getCustomerId(), customerName, s.getWarehouseId(), s.getStatus(),
                s.getShippedAt(), s.getPackageCount(), lineCount);
    }
}

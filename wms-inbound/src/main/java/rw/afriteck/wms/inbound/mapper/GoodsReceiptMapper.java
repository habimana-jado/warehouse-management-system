package rw.afriteck.wms.inbound.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptLineResponse;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptResponse;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.entity.GoodsReceipt;
import rw.afriteck.wms.inbound.entity.GoodsReceiptLine;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GoodsReceiptMapper {

    private GoodsReceiptMapper() {}

    /** locationCodes: every location id used by the lines (receiving, reject, suggested, put-away). */
    public static GoodsReceiptResponse toResponse(GoodsReceipt r, String poNumber,
                                                  Map<UUID, ItemResponse> items,
                                                  Map<UUID, String> locationCodes,
                                                  Map<UUID, String> userNames) {
        List<GoodsReceiptLineResponse> lines = r.getLines().stream()
                .map(l -> toLineResponse(l, items, locationCodes))
                .toList();
        return new GoodsReceiptResponse(
                r.getId(), r.getVersion(), r.getReceiptNumber(),
                r.getPurchaseOrder().getId(), poNumber,
                r.getWarehouseId(),
                r.getMode(),
                r.getReceivedAt(), r.getReceivedBy(), userNames.get(r.getReceivedBy()),
                r.getDeliveryNoteNumber(), r.getNotes(),
                r.getTaskId(),
                lines);
    }

    public static GoodsReceiptLineResponse toLineResponse(GoodsReceiptLine l,
                                                          Map<UUID, ItemResponse> items,
                                                          Map<UUID, String> locationCodes) {
        ItemResponse item = items.get(l.getItemId());
        return new GoodsReceiptLineResponse(
                l.getId(), l.getLineNo(), l.getPurchaseOrderLine().getId(),
                l.getItemId(),
                item == null ? null : item.sku(),
                item == null ? null : item.name(),
                item == null ? null : item.baseUom(),
                l.getLotNumber(), l.getExpiryDate(), l.getSerialNumber(),
                l.getAcceptedQty(), l.getRejectedQty(), l.getRejectReason(),
                l.getReceivingLocationId(), code(locationCodes, l.getReceivingLocationId()),
                l.getRejectLocationId(), code(locationCodes, l.getRejectLocationId()),
                l.getUnitCost(),
                l.getPutawayStatus(),
                l.getSuggestedLocationId(), code(locationCodes, l.getSuggestedLocationId()),
                l.getPutawayLocationId(), code(locationCodes, l.getPutawayLocationId()),
                l.getPutawayDoneQty(),
                l.getTaskLineId());
    }

    public static GoodsReceiptSummaryResponse toSummary(GoodsReceipt r, String poNumber, String receivedByName,
                                                        int lineCount, int pendingPutawayCount) {
        return new GoodsReceiptSummaryResponse(
                r.getId(), r.getReceiptNumber(),
                r.getPurchaseOrder().getId(), poNumber,
                r.getWarehouseId(),
                r.getMode(),
                r.getReceivedAt(), r.getReceivedBy(), receivedByName,
                r.getDeliveryNoteNumber(),
                lineCount, pendingPutawayCount);
    }

    private static String code(Map<UUID, String> codes, UUID id) {
        return id == null ? null : codes.get(id);
    }
}
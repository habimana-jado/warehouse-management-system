package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.inventory.dtos.TransferLineResponse;
import rw.afriteck.wms.inventory.dtos.TransferResponse;
import rw.afriteck.wms.inventory.dtos.TransferSummaryResponse;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockTransfer;
import rw.afriteck.wms.inventory.enums.TransferStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TransferMapper {
    private TransferMapper() {}

    public static TransferResponse toResponse(StockTransfer t, Map<UUID, ItemResponse> items,
                                              Map<UUID, LocationResponse> locations) {
        boolean inTransit = t.getStatus() == TransferStatus.IN_TRANSIT;
        List<TransferLineResponse> lines = t.getLines().stream().map(l -> {
            ItemResponse item = items.get(l.getItemId());
            LocationResponse from = locations.get(l.getFromLocationId());
            LocationResponse to = l.getToLocationId() == null ? null : locations.get(l.getToLocationId());
            Lot lot = l.getLot();
            return new TransferLineResponse(l.getId(), l.getLineNo(),
                    l.getItemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                    lot == null ? null : lot.getId(), lot == null ? null : lot.getLotNumber(),
                    lot == null ? null : lot.getExpiryDate(), l.getSerialNumber(),
                    l.getFromLocationId(), from == null ? null : from.code(),
                    l.getToLocationId(), to == null ? null : to.code(),
                    l.getQuantity(), l.getShippedQty(), l.getReceivedQty(),
                    inTransit ? l.getShippedQty().subtract(l.getReceivedQty()) : BigDecimal.ZERO,
                    l.getShortReason());
        }).toList();
        return new TransferResponse(t.getId(), t.getVersion(), t.getTransferNumber(), t.getType(), t.getStatus(),
                t.getSourceWarehouseId(), t.getDestinationWarehouseId(), t.getShippedAt(), t.getCompletedAt(),
                t.getCancelReason(), t.getNotes(), t.getCreatedAt(), lines);
    }

    public static TransferSummaryResponse toSummary(StockTransfer t, int lineCount) {
        return new TransferSummaryResponse(t.getId(), t.getTransferNumber(), t.getType(), t.getStatus(),
                t.getSourceWarehouseId(), t.getDestinationWarehouseId(), lineCount,
                t.getShippedAt(), t.getCompletedAt(), t.getCreatedAt());
    }
}

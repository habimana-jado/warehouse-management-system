package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.dtos.MovementResponse;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockMovement;

public final class MovementMapper {
    private MovementMapper() {}

    public static MovementResponse toResponse(StockMovement m) {
        Lot lot = m.getLot();
        return new MovementResponse(m.getId(), m.getTransactionId(), m.getType(), m.getOccurredAt(),
                m.getWarehouseId(), m.getLocationId(), m.getItemId(),
                lot == null ? null : lot.getId(),
                lot == null ? null : lot.getLotNumber(),
                lot == null ? null : lot.getExpiryDate(),
                m.getSerialNumber(), m.getQuantity(), m.getBalanceAfter(), m.getUnitCost(),
                m.getReferenceType(), m.getReferenceId(), m.getReferenceNumber(), m.getNote());
    }

    public static MovementHistoryResponse toHistory(StockMovement m, ItemResponse item, LocationResponse loc) {
        Lot lot = m.getLot();
        return new MovementHistoryResponse(m.getId(), m.getTransactionId(), m.getType(), m.getOccurredAt(),
                m.getWarehouseId(), m.getLocationId(), loc == null ? null : loc.code(),
                m.getItemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                lot == null ? null : lot.getId(), lot == null ? null : lot.getLotNumber(),
                lot == null ? null : lot.getExpiryDate(), m.getSerialNumber(),
                m.getQuantity(), m.getBalanceAfter(), m.getUnitCost(),
                m.getReferenceType(), m.getReferenceId(), m.getReferenceNumber(), m.getNote());
    }
}

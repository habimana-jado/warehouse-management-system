package rw.afriteck.wms.inventory.mapper;

import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.inventory.dtos.CountLineResponse;
import rw.afriteck.wms.inventory.dtos.CountResponse;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockCount;
import rw.afriteck.wms.inventory.entity.StockCountLine;

import java.math.BigDecimal;
import java.util.*;

public final class CountMapper {
    private CountMapper() {}

    public static CountResponse toResponse(StockCount c, long total, long counted, Long variance, Map<UUID, String> names) {
        return new CountResponse(c.getId(), c.getVersion(), c.getCountNumber(), c.getWarehouseId(),
                c.getZoneLocationId(), c.getStatus(), c.getStartedAt(), c.getCompletedAt(),
                c.getApprovedBy(), c.getApprovedBy() == null ? null : names.get(c.getApprovedBy()),
                c.getCancelReason(), c.getNotes(), c.getCreatedAt(), total, counted, variance);
    }

    public static CountLineResponse toLine(StockCountLine l, ItemResponse item, LocationResponse loc, Map<UUID, String> names) {
        Lot lot = l.getLot();
        BigDecimal variance = l.getCountedQty() == null ? null : l.getCountedQty().subtract(l.getExpectedQty());
        return new CountLineResponse(l.getId(), l.getLineNo(),
                l.getLocationId(), loc == null ? null : loc.code(),
                l.getItemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                lot == null ? null : lot.getId(), lot == null ? null : lot.getLotNumber(),
                lot == null ? null : lot.getExpiryDate(), l.getSerialNumber(),
                l.getExpectedQty(), l.getCountedQty(), variance, l.getStatus(),
                l.getCountedBy(), l.getCountedBy() == null ? null : names.get(l.getCountedBy()), l.getCountedAt());
    }
}
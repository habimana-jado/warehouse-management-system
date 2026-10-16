package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.outbound.enums.PickLineStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PickLineResponse(UUID id, int lineNo, UUID salesOrderLineId,
                               UUID itemId, String itemSku, String itemName, String baseUom, TrackingType trackingType, boolean expiryTracked,
                               BigDecimal plannedQty, BigDecimal pickedQty, PickLineStatus status,
                               UUID suggestedLocationId, String suggestedLocationCode, String suggestedLotNumber,
                               String shortReason, List<PickAllocationResponse> allocations) {}

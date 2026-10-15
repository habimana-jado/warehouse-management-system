package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.PutawayStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoodsReceiptLineResponse(
        UUID id, int lineNo, UUID purchaseOrderLineId,
        UUID itemId, String itemSku, String itemName, String baseUom,
        String lotNumber, LocalDate expiryDate, String serialNumber,
        BigDecimal acceptedQty, BigDecimal rejectedQty, String rejectReason,
        UUID receivingLocationId, String receivingLocationCode,
        UUID rejectLocationId, String rejectLocationCode,
        BigDecimal unitCost,
        PutawayStatus putawayStatus,
        UUID suggestedLocationId, String suggestedLocationCode,
        UUID putawayLocationId, String putawayLocationCode,
        BigDecimal putawayDoneQty,
        UUID taskLineId) {}
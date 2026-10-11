package rw.afriteck.wms.operations.dtos;

import rw.afriteck.wms.operations.enums.TaskLineStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TaskLineResponse(
        UUID id, int lineNo,
        UUID itemId, String itemSku, String itemName,
        String lotNumber, String serialNumber,
        UUID fromLocationId, String fromLocationCode,
        UUID toLocationId, String toLocationCode,
        BigDecimal plannedQty, BigDecimal doneQty,
        TaskLineStatus status, UUID referenceLineId, String shortReason
) {}

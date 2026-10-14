package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferLineResponse(
        UUID id, int lineNo, UUID itemId, String itemSku, String itemName,
        UUID lotId, String lotNumber, LocalDate expiryDate, String serialNumber,
        UUID fromLocationId, String fromLocationCode, UUID toLocationId, String toLocationCode,
        BigDecimal quantity, BigDecimal shippedQty, BigDecimal receivedQty, BigDecimal inTransitQty,
        String shortReason) {}

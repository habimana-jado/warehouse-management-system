package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.ReturnDisposition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerReturnLineResponse(
        UUID id, int lineNo,
        UUID itemId, String itemSku, String itemName, String baseUom,
        BigDecimal expectedQty, BigDecimal receivedQty,
        String lotNumber, LocalDate expiryDate, String serialNumber,
        String returnReason,
        ReturnDisposition disposition,
        UUID locationId, String locationCode) {}
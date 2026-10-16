package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record StockSummaryResponse(UUID itemId, String itemSku, String itemName, String baseUom,
                                   BigDecimal onHand, BigDecimal storageQty, BigDecimal receivingQty, BigDecimal damagedQty, BigDecimal otherQty) {}


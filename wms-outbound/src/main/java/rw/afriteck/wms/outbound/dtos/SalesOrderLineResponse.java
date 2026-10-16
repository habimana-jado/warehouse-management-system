package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderLineResponse(UUID id, int lineNo, UUID itemId, String itemSku, String itemName, String baseUom,
                                     BigDecimal orderedQty, BigDecimal releasedQty, BigDecimal pickedQty, BigDecimal shippedQty,
                                     BigDecimal unreleasedQty, BigDecimal readyToShipQty,
                                     BigDecimal unitPrice, BigDecimal lineTotal, String note) {}                  // lineTotal null when unitPrice is null

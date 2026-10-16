package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentLineResponse(UUID id, int lineNo, UUID salesOrderLineId,
                                   UUID itemId, String itemSku, String itemName, String baseUom, BigDecimal quantity) {}


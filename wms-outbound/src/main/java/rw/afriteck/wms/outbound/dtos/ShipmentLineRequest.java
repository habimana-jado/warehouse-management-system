package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ShipmentLineRequest(UUID salesOrderLineId, BigDecimal quantity) {}


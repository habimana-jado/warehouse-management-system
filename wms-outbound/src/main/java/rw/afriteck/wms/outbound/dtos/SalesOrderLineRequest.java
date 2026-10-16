package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesOrderLineRequest(UUID itemId, BigDecimal quantity, String uom, BigDecimal unitPrice, String note) {}
//   uom null = base UOM; unitPrice is per `uom`; the service converts and preserves the line total

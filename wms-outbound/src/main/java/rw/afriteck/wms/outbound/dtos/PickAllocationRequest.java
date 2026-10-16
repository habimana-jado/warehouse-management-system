package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record PickAllocationRequest(UUID locationId, String lotNumber, String serialNumber, BigDecimal quantity) {}
//   one row per bin and lot, or one row per serial unit (quantity 1); base UOM


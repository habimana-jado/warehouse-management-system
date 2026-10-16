package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record PickAllocationResponse(UUID id, UUID locationId, String locationCode, String lotNumber,
                                     String serialNumber, BigDecimal quantity, boolean expiryOverride) {}

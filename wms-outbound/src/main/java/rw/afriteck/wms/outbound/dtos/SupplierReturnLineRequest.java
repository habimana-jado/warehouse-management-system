package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierReturnLineRequest(UUID itemId, UUID locationId, String lotNumber, String serialNumber,
                                        BigDecimal quantity, String uom, String note) {}   // a SERIAL item needs one line per unit

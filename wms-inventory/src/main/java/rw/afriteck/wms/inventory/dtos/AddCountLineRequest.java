package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record AddCountLineRequest(UUID locationId, UUID itemId, String lotNumber, String serialNumber,
                                  BigDecimal countedQty) {}

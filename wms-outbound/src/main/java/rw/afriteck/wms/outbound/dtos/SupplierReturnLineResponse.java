package rw.afriteck.wms.outbound.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierReturnLineResponse(UUID id, int lineNo, UUID itemId, String itemSku, String itemName, String baseUom,
                                         UUID locationId, String locationCode, String lotNumber, String serialNumber, BigDecimal quantity, String note) {}

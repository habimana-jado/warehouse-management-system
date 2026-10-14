package rw.afriteck.wms.inventory.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferLineRequest(
        UUID itemId,
        String lotNumber,                   // required for LOT items, must already exist
        String serialNumber,                // required for SERIAL items
        UUID fromLocationId,                // active BIN of the source warehouse
        UUID toLocationId,                  // BIN transfers: required. INTER_WAREHOUSE: must be null (chosen on receiving)
        BigDecimal quantity) {}             // > 0, base UOM

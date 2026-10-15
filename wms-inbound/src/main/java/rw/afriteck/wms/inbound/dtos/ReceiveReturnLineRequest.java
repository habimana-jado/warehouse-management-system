package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import rw.afriteck.wms.inbound.enums.ReturnDisposition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiveReturnLineRequest(
        @NotNull UUID lineId,
        String uom,                                // null/blank = base UOM
        @NotNull @DecimalMin("0") BigDecimal receivedQty,
        ReturnDisposition disposition,             // required when receivedQty > 0
        UUID locationId,                           // RESTOCK: STORAGE bin (null -> suggested); DAMAGED: DAMAGED bin (null -> first)
        String lotNumber,                          // optional: fills in or corrects the expected value
        LocalDate expiryDate,
        String serialNumber) {}
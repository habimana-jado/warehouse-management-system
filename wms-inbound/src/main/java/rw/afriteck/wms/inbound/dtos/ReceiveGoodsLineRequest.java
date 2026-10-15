package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReceiveGoodsLineRequest(
        @NotNull UUID purchaseOrderLineId,
        String uom,                                // null/blank = base UOM
        @DecimalMin("0") BigDecimal acceptedQty,   // in `uom`; null -> 0
        @DecimalMin("0") BigDecimal rejectedQty,   // in `uom`; null -> 0
        @Size(max = 500) String rejectReason,      // required by the service when rejectedQty > 0
        @Size(max = 100) String lotNumber,         // normalized by the service (Codes.normalize)
        LocalDate expiryDate,
        @Size(max = 100) String serialNumber,
        UUID locationId) {}                        // DIRECT only: STORAGE bin for the accepted stock; null -> suggested bin
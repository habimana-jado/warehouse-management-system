package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePurchaseOrderRequest(
        @NotNull UUID supplierId,
        @NotNull UUID warehouseId,
        LocalDate orderDate,                    // null -> today
        LocalDate expectedDate,
        @Size(max = 100) String supplierReference,
        @Size(max = 3) String currency,         // null/blank -> RWF; normalized with Codes.normalize
        @Size(max = 1000) String notes,
        @Valid List<PurchaseOrderLineRequest> lines) {}   // may be empty in DRAFT; confirm needs at least one
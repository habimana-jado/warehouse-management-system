package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** DRAFT only. `lines` is the COMPLETE desired list: lines with an id are kept and updated, lines without an id
 *  are added, existing lines that are missing are removed (diff-based sync in the service). */
public record UpdatePurchaseOrderRequest(
        LocalDate expectedDate,
        @Size(max = 100) String supplierReference,
        @Size(max = 3) String currency,
        @Size(max = 1000) String notes,
        @Valid List<PurchaseOrderLineRequest> lines) {}
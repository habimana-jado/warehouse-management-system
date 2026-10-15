package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Lines that are not listed are treated as "not returned": received 0, no stock posted. */
public record ReceiveCustomerReturnRequest(
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<ReceiveReturnLineRequest> lines) {}
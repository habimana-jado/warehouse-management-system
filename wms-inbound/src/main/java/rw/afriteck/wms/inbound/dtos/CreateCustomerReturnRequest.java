package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateCustomerReturnRequest(
        @NotNull UUID warehouseId,
        UUID customerId,                           // optional (walk-in returns)
        UUID salesOrderId,                         // optional plain reference; outbound is another module
        @Size(max = 50) String salesOrderNumber,
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<CustomerReturnLineRequest> lines) {}
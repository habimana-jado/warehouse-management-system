package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelPurchaseOrderRequest(@NotBlank @Size(max = 500) String reason) {}
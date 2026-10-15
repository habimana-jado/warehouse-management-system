package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmPutawayRequest(
        UUID locationId,                           // the bin the worker actually used; null -> suggested bin
        @DecimalMin("0") BigDecimal doneQty,       // base UOM; null -> the full accepted quantity
        @Size(max = 500) String shortReason) {}    // required when doneQty is below the planned quantity
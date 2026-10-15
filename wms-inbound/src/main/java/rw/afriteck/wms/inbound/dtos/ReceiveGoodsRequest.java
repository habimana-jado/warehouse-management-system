package rw.afriteck.wms.inbound.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import rw.afriteck.wms.inbound.enums.ReceiptMode;

import java.util.List;
import java.util.UUID;

public record ReceiveGoodsRequest(
        ReceiptMode mode,                          // null -> PUTAWAY
        UUID receivingLocationId,                  // PUTAWAY: the RECEIVING bin; null -> first active RECEIVING bin
        UUID damagedLocationId,                    // bin for rejected goods; null -> first active DAMAGED bin
        @Size(max = 100) String deliveryNoteNumber,
        @Size(max = 1000) String notes,
        @NotEmpty @Valid List<ReceiveGoodsLineRequest> lines) {}
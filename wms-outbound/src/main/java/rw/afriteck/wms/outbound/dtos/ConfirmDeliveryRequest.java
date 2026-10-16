package rw.afriteck.wms.outbound.dtos;

import java.time.Instant;

public record ConfirmDeliveryRequest(Instant deliveredAt, String receivedByName, String podNotes) {}   // deliveredAt null -> now


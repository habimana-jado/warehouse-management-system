package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PutawayBacklogRowResponse(UUID receiptId, String receiptNumber, Instant receivedAt, long ageHours,
                                        UUID receiptLineId, UUID itemId, String itemSku, String itemName, BigDecimal acceptedQty,
                                        UUID suggestedLocationId, String suggestedLocationCode) {}

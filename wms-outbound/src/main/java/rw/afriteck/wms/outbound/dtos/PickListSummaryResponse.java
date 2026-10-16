package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.PickListStatus;

import java.time.Instant;
import java.util.UUID;

public record PickListSummaryResponse(UUID id, String pickNumber, UUID salesOrderId, String orderNumber,
                                      UUID warehouseId, PickListStatus status, UUID taskId, Instant releasedAt,
                                      int lineCount, int doneLineCount) {}                                         // done = lines no longer PENDING

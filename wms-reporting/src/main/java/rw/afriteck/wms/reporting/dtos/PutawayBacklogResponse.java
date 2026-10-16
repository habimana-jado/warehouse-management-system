package rw.afriteck.wms.reporting.dtos;

import rw.afriteck.wms.common.dtos.PageResponse;

import java.time.Instant;
import java.util.UUID;

public record PutawayBacklogResponse(UUID warehouseId, long pendingLines, Instant oldestReceivedAt,
                                     PageResponse<PutawayBacklogRowResponse> rows) {}

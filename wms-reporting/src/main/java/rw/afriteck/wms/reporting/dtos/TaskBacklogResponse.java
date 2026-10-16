package rw.afriteck.wms.reporting.dtos;

import rw.afriteck.wms.common.dtos.PageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskBacklogResponse(UUID warehouseId, Instant asOf, long overdueTasks,
                                  List<TaskBacklogRowResponse> byType, PageResponse<OverdueTaskRowResponse> overdue) {}

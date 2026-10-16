package rw.afriteck.wms.reporting.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdjustmentsReportResponse(UUID warehouseId, LocalDate from, LocalDate toExclusive,
                                        long postedAdjustments, long pendingApprovals, Instant oldestPendingAt,
                                        List<AdjustmentReasonRowResponse> byReason) {}

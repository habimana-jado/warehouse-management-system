package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.reporting.dtos.AdjustmentsReportResponse;
import rw.afriteck.wms.reporting.dtos.AuditFeedResponse;
import rw.afriteck.wms.reporting.dtos.CountAccuracyResponse;
import rw.afriteck.wms.reporting.dtos.TaskBacklogResponse;
import rw.afriteck.wms.reporting.enums.AuditSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AccuracyReportService {
    CountAccuracyResponse countAccuracy(UUID warehouseId, LocalDate from, LocalDate to, int topItems);
    AdjustmentsReportResponse adjustments(UUID warehouseId, LocalDate from, LocalDate to);
}


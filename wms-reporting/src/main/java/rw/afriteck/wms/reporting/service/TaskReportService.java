package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.reporting.dtos.TaskBacklogResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskReportService {
    TaskBacklogResponse backlog(UUID warehouseId, int page, int size);
    List<WorkerProductivityResponse> productivity(UUID warehouseId, LocalDate from, LocalDate to, TaskType type);
}

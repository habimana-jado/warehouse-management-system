package rw.afriteck.wms.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.reporting.dtos.TaskBacklogResponse;
import rw.afriteck.wms.reporting.service.TaskReportService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/tasks")
@RequiredArgsConstructor
class TaskReportController {

    private final TaskReportService service;
    private final UserContext userContext;

    @GetMapping("/backlog")
    @PreAuthorize(ReportingAccess.BOARD)
    TaskBacklogResponse backlog(@RequestParam UUID warehouseId,
                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.backlog(warehouseId, page, size);
    }

    @GetMapping("/productivity")
    @PreAuthorize(ReportingAccess.MANAGER)
    List<WorkerProductivityResponse> productivity(@RequestParam UUID warehouseId,
                                                  @RequestParam(required = false) TaskType type,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.productivity(warehouseId, from, to, type);
    }
}

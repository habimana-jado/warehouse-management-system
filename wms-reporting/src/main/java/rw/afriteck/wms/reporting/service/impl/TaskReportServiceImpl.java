package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.operations.dtos.ProductivityFilter;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskProductivityService;
import rw.afriteck.wms.reporting.dtos.*;
import rw.afriteck.wms.reporting.repository.TaskReportRepository;
import rw.afriteck.wms.reporting.service.TaskReportService;
import rw.afriteck.wms.reporting.support.Paging;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskReportServiceImpl implements TaskReportService {

    private final TaskReportRepository repository;
    private final TaskProductivityService productivity;
    private final ReportLookups lookups;
    private final ReportClock clock;

    @Override
    public TaskBacklogResponse backlog(UUID warehouseId, int page, int size) {
        Instant now = clock.now();
        int s = Paging.size(size), p = Paging.page(page);
        var byType = repository.backlogByType(warehouseId, now).stream()
                .map(b -> new TaskBacklogRowResponse(b.type(), b.open(), b.assigned(), b.inProgress(), b.overdue()))
                .toList();
        long overdueTotal = byType.stream().mapToLong(TaskBacklogRowResponse::overdue).sum();

        var rows = repository.findOverdue(warehouseId, now, s, (long) p * s);
        Map<UUID, String> names = lookups.workerNames(rows.stream()
                .map(TaskReportRepository.OverdueRow::assigneeId).toList());
        List<OverdueTaskRowResponse> content = rows.stream().map(t -> new OverdueTaskRowResponse(
                t.id(), t.taskNumber(), t.type(), t.status(), t.priority(), t.referenceType(),
                t.referenceNumber(), t.assigneeId(), t.assigneeId() == null ? null : names.get(t.assigneeId()),
                t.dueAt(), Duration.between(t.dueAt(), now).toHours(), t.createdAt())).toList();

        return new TaskBacklogResponse(warehouseId, now, overdueTotal, byType,
                new PageResponse<>(content, p, s, overdueTotal, Paging.totalPages(overdueTotal, s)));
    }

    @Override
    public List<WorkerProductivityResponse> productivity(UUID warehouseId, LocalDate from, LocalDate to,
                                                         TaskType type) {
        ReportClock.DayRange r = clock.dayRange(from, to, 30);
        return productivity.byWorker(new ProductivityFilter(warehouseId,
                clock.startOfDay(r.from()), clock.startOfDay(r.to()), type));
    }
}

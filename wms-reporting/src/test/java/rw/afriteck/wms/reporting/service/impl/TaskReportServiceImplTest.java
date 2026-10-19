package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.operations.dtos.ProductivityFilter;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskProductivityService;
import rw.afriteck.wms.reporting.dtos.TaskBacklogResponse;
import rw.afriteck.wms.reporting.repository.TaskReportRepository;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskReportServiceImplTest {

    @Mock
    private TaskReportRepository repository;
    @Mock
    private TaskProductivityService productivity;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;

    @InjectMocks
    private TaskReportServiceImpl service;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
    }

    @Test
    void backlog() {
        Instant now = Instant.parse("2026-05-15T12:00:00Z");
        when(clock.now()).thenReturn(now);

        TaskReportRepository.BacklogRow typeRow = new TaskReportRepository.BacklogRow(
                "PICK", 10L, 5L, 2L, 1L);
        UUID taskId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        Instant dueAt = now.minusSeconds(3600); // 1 hour overdue

        TaskReportRepository.OverdueRow overdueRow = new TaskReportRepository.OverdueRow(
                taskId, "TSK-001", "PICK", "OPEN", "HIGH",
                "SO", "SO-100", workerId, dueAt, now.minusSeconds(7200));

        when(repository.backlogByType(warehouseId, now)).thenReturn(List.of(typeRow));
        when(repository.findOverdue(warehouseId, now, 50, 0L)).thenReturn(List.of(overdueRow));
        when(lookups.workerNames(any())).thenReturn(Map.of(workerId, "Worker 1"));

        TaskBacklogResponse resp = service.backlog(warehouseId, 0, 50);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(now, resp.asOf());
        assertEquals(1L, resp.overdueTasks());
        assertEquals(1, resp.byType().size());
        assertEquals(1, resp.overdue().content().size());
        assertEquals(1L, resp.overdue().content().get(0).hoursOverdue());
    }

    @Test
    void productivity() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        when(clock.dayRange(from, to, 30)).thenReturn(new ReportClock.DayRange(from, to));

        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-05-15T00:00:00Z");
        when(clock.startOfDay(from)).thenReturn(start);
        when(clock.startOfDay(to)).thenReturn(end);

        UUID workerId = UUID.randomUUID();
        WorkerProductivityResponse prod = new WorkerProductivityResponse(
                workerId, "Worker 1", 10L, 50L, BigDecimal.valueOf(100), 0L, 3600L);

        when(productivity.byWorker(any(ProductivityFilter.class))).thenReturn(List.of(prod));

        List<WorkerProductivityResponse> list = service.productivity(warehouseId, from, to, TaskType.PICK);

        assertEquals(1, list.size());
        assertEquals("Worker 1", list.get(0).userName());
    }
}

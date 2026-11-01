package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.reporting.dtos.TaskBacklogResponse;
import rw.afriteck.wms.reporting.service.TaskReportService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskReportControllerTest {

    @Mock
    private TaskReportService service;
    @Mock
    private UserContext userContext;

    private TaskReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new TaskReportController(service, userContext);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void backlog_delegatesToServiceAndChecksUserContext() {
        TaskBacklogResponse expected = new TaskBacklogResponse(
                warehouseId, Instant.now(), 0L, List.of(), new PageResponse<>(List.of(), 0, 50, 0L, 0));

        when(service.backlog(warehouseId, 0, 50)).thenReturn(expected);

        var actual = controller.backlog(warehouseId, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).backlog(warehouseId, 0, 50);
    }

    @Test
    void productivity_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        List<WorkerProductivityResponse> expected = List.of();

        when(service.productivity(warehouseId, from, to, TaskType.PICK)).thenReturn(expected);

        var actual = controller.productivity(warehouseId, TaskType.PICK, from, to);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).productivity(warehouseId, from, to, TaskType.PICK);
    }
}

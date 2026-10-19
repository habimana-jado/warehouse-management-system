package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.AuditFeedResponse;
import rw.afriteck.wms.reporting.enums.AuditSource;
import rw.afriteck.wms.reporting.service.AuditReportService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditReportControllerTest {

    @Mock
    private AuditReportService service;
    @Mock
    private UserContext userContext;

    private AuditReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new AuditReportController(service, userContext);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void feed_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 10);
        Set<AuditSource> sources = Set.of(AuditSource.STOCK_MOVEMENT);
        UUID actorId = UUID.randomUUID();

        AuditFeedResponse expected = new AuditFeedResponse(List.of(), 0, 50, false);

        when(service.feed(warehouseId, from, to, sources, actorId, 0, 50)).thenReturn(expected);

        AuditFeedResponse actual = controller.feed(warehouseId, from, to, sources, actorId, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).feed(warehouseId, from, to, sources, actorId, 0, 50);
    }
}

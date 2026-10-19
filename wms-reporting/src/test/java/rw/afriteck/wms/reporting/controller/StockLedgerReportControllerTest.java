package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.reporting.service.StockLedgerReportService;
import rw.afriteck.wms.reporting.support.ReportClock;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLedgerReportControllerTest {

    @Mock
    private StockLedgerReportService ledgerService;
    @Mock
    private UserContext userContext;
    @Mock
    private ReportClock clock;

    private StockLedgerReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new StockLedgerReportController(ledgerService, userContext, clock);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void export_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        when(clock.range(from, to)).thenReturn(new ReportClock.DateRange(null, null));
        when(clock.today()).thenReturn(to);

        byte[] csvData = "occurred_at,type...".getBytes();
        when(ledgerService.exportCsv(any(MovementFilter.class))).thenReturn(csvData);

        ResponseEntity<byte[]> response = controller.export(warehouseId, null, null, null, null, null, from, to, null, null);

        assertNotNull(response);
        assertArrayEquals(csvData, response.getBody());
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(ledgerService).exportCsv(any(MovementFilter.class));
    }
}

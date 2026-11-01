package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.reporting.dtos.ExpiryRowResponse;
import rw.afriteck.wms.reporting.dtos.StockSummaryResponse;
import rw.afriteck.wms.reporting.dtos.ValuationReportResponse;
import rw.afriteck.wms.reporting.service.ExpiryReportService;
import rw.afriteck.wms.reporting.service.LowStockReportService;
import rw.afriteck.wms.reporting.service.StockSummaryService;
import rw.afriteck.wms.reporting.service.ValuationReportService;
import rw.afriteck.wms.reporting.support.ReportClock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockReportControllerTest {

    @Mock
    private StockSummaryService summaryService;
    @Mock
    private ExpiryReportService expiryService;
    @Mock
    private ValuationReportService valuationService;
    @Mock
    private LowStockReportService lowStockService;
    @Mock
    private UserContext userContext;
    @Mock
    private ReportClock clock;

    private StockReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new StockReportController(summaryService, expiryService, valuationService, lowStockService, userContext, clock);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void summary_delegatesToServiceAndChecksUserContext() {
        PageResponse<StockSummaryResponse> expected = new PageResponse<>(List.of(), 0, 50, 0L, 0);
        when(summaryService.summary(warehouseId, "q", 0, 50)).thenReturn(expected);

        var actual = controller.summary(warehouseId, "q", 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(summaryService).summary(warehouseId, "q", 0, 50);
    }

    @Test
    void expiry_delegatesToServiceAndChecksUserContext() {
        PageResponse<ExpiryRowResponse> expected = new PageResponse<>(List.of(), 0, 50, 0L, 0);
        when(expiryService.expiry(warehouseId, 30, true, 0, 50)).thenReturn(expected);

        var actual = controller.expiry(warehouseId, 30, true, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(expiryService).expiry(warehouseId, 30, true, 0, 50);
    }

    @Test
    void valuation_delegatesToServiceAndChecksUserContext() {
        ValuationReportResponse expected = new ValuationReportResponse(
                warehouseId, "RWF", Instant.now(), BigDecimal.ZERO, 0, 0, BigDecimal.ZERO,
                new PageResponse<>(List.of(), 0, 50, 0L, 0));
        when(valuationService.valuation(warehouseId, null, 0, 50)).thenReturn(expected);

        var actual = controller.valuation(warehouseId, null, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(valuationService).valuation(warehouseId, null, 0, 50);
    }

    @Test
    void lowStock_delegatesToServiceAndChecksUserContext() {
        List<LowStockResponse> expected = List.of();
        when(lowStockService.list(warehouseId)).thenReturn(expected);

        var actual = controller.lowStock(warehouseId);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(lowStockService).list(warehouseId);
    }

    @Test
    void lowStockCsv_delegatesToServiceAndChecksUserContext() {
        byte[] csvData = "sku,name...".getBytes();
        when(clock.today()).thenReturn(LocalDate.of(2026, 5, 15));
        when(lowStockService.csv(warehouseId)).thenReturn(csvData);

        ResponseEntity<byte[]> actual = controller.lowStockCsv(warehouseId);

        assertNotNull(actual);
        assertArrayEquals(csvData, actual.getBody());
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(lowStockService).csv(warehouseId);
    }
}

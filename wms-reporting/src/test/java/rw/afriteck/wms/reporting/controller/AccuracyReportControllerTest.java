package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.AdjustmentsReportResponse;
import rw.afriteck.wms.reporting.dtos.CountAccuracyResponse;
import rw.afriteck.wms.reporting.service.AccuracyReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccuracyReportControllerTest {

    @Mock
    private AccuracyReportService service;
    @Mock
    private UserContext userContext;

    private AccuracyReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new AccuracyReportController(service, userContext);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void counts_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 1);
        CountAccuracyResponse expected = new CountAccuracyResponse(
                warehouseId, from, to, 1L, 10L, 8L, new BigDecimal("80.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());

        when(service.countAccuracy(warehouseId, from, to, 10)).thenReturn(expected);

        CountAccuracyResponse actual = controller.counts(warehouseId, from, to, 10);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).countAccuracy(warehouseId, from, to, 10);
    }

    @Test
    void adjustments_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        AdjustmentsReportResponse expected = new AdjustmentsReportResponse(
                warehouseId, from, to, 5L, 2L, null, List.of());

        when(service.adjustments(warehouseId, from, to)).thenReturn(expected);

        AdjustmentsReportResponse actual = controller.adjustments(warehouseId, from, to);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).adjustments(warehouseId, from, to);
    }
}

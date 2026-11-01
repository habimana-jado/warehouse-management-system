package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.reporting.dtos.AdjustmentsReportResponse;
import rw.afriteck.wms.reporting.dtos.CountAccuracyResponse;
import rw.afriteck.wms.reporting.repository.AccuracyReportRepository;
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
class AccuracyReportServiceImplTest {

    @Mock
    private AccuracyReportRepository repository;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;

    @InjectMocks
    private AccuracyReportServiceImpl service;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
    }

    @Test
    void countAccuracy() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 1);
        ReportClock.DayRange range = new ReportClock.DayRange(from, to);

        when(clock.dayRange(from, to, 90)).thenReturn(range);
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-04-01T00:00:00Z");
        when(clock.startOfDay(from)).thenReturn(start);
        when(clock.startOfDay(to)).thenReturn(end);

        UUID countId = UUID.randomUUID();
        UUID zoneLocId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        AccuracyReportRepository.CountRow countRow = new AccuracyReportRepository.CountRow(
                countId, "CNT-001", zoneLocId, Instant.now(), workerId,
                10L, 8L, 1L, 1L, BigDecimal.ZERO, new BigDecimal("2"));
        AccuracyReportRepository.VarianceRow varianceRow = new AccuracyReportRepository.VarianceRow(
                itemId, 5L, new BigDecimal("3"), new BigDecimal("3"));

        when(repository.countRows(warehouseId, start, end)).thenReturn(List.of(countRow));
        when(repository.topVarianceItems(warehouseId, start, end, 10)).thenReturn(List.of(varianceRow));

        LocationResponse loc = new LocationResponse(zoneLocId, warehouseId, null, "Zone A", "ZA", LocationType.ZONE, LocationUsage.STORAGE, null, null, true);
        ItemResponse item = new ItemResponse(itemId, "SKU1", "Item 1", null, null, "Cat", "PCS", TrackingType.NONE, false, true);

        when(lookups.locationsById(any())).thenReturn(Map.of(zoneLocId, loc));
        when(lookups.workerNames(any())).thenReturn(Map.of(workerId, "Worker 1"));
        when(lookups.itemsById(any())).thenReturn(Map.of(itemId, item));

        CountAccuracyResponse resp = service.countAccuracy(warehouseId, from, to, 10);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(1L, resp.counts());
        assertEquals(10L, resp.lines());
        assertEquals(8L, resp.matchingLines());
        assertEquals(new BigDecimal("80.00"), resp.lineAccuracy());
        assertEquals(1, resp.countList().size());
        assertEquals(1, resp.topVarianceItems().size());
    }

    @Test
    void adjustments() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        ReportClock.DayRange range = new ReportClock.DayRange(from, to);

        when(clock.dayRange(from, to, 30)).thenReturn(range);
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-05-15T00:00:00Z");
        when(clock.startOfDay(from)).thenReturn(start);
        when(clock.startOfDay(to)).thenReturn(end);

        AccuracyReportRepository.ReasonRow reasonRow = new AccuracyReportRepository.ReasonRow(
                "DAMAGED", "Damaged Goods", 5L, 10L, BigDecimal.ZERO, new BigDecimal("10"));
        AccuracyReportRepository.PendingRow pendingRow = new AccuracyReportRepository.PendingRow(
                2L, Instant.now());

        when(repository.adjustmentsByReason(warehouseId, start, end)).thenReturn(List.of(reasonRow));
        when(repository.pendingAdjustments(warehouseId)).thenReturn(pendingRow);

        AdjustmentsReportResponse resp = service.adjustments(warehouseId, from, to);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(5L, resp.postedAdjustments());
        assertEquals(2L, resp.pendingApprovals());
        assertEquals(1, resp.byReason().size());
    }
}

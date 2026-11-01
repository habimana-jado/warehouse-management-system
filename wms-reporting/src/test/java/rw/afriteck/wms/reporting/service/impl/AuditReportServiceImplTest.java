package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.reporting.dtos.AuditFeedResponse;
import rw.afriteck.wms.reporting.enums.AuditSource;
import rw.afriteck.wms.reporting.repository.AuditReportRepository;
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
class AuditReportServiceImplTest {

    @Mock
    private AuditReportRepository repository;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;

    @InjectMocks
    private AuditReportServiceImpl service;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
    }

    @Test
    void feed_dateRangeTooLarge_throwsException() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 1);
        when(clock.dayRange(from, to, 7)).thenReturn(new ReportClock.DayRange(from, to));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.feed(warehouseId, from, to, null, null, 0, 50));
        assertEquals("DATE_RANGE_TOO_LARGE", ex.getCode());
    }

    @Test
    void feed_success() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 10);
        when(clock.dayRange(from, to, 7)).thenReturn(new ReportClock.DayRange(from, to));

        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-05-10T00:00:00Z");
        when(clock.startOfDay(from)).thenReturn(start);
        when(clock.startOfDay(to)).thenReturn(end);

        UUID actorId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        AuditReportRepository.AuditRow row = new AuditReportRepository.AuditRow(
                Instant.now(), "STOCK_MOVEMENT", "RECEIPT", actorId, itemId, null,
                BigDecimal.TEN, "PO", UUID.randomUUID(), "PO-100", "Received items");

        when(repository.search(eq(warehouseId), eq(start), eq(end), any(), eq(null), eq(51), eq(0L)))
                .thenReturn(List.of(row));

        when(lookups.workerNames(any())).thenReturn(Map.of(actorId, "Alice"));

        AuditFeedResponse resp = service.feed(warehouseId, from, to, null, null, 0, 50);

        assertEquals(1, resp.content().size());
        assertFalse(resp.hasMore());
        assertEquals("Alice", resp.content().get(0).actorName());
        assertEquals(AuditSource.STOCK_MOVEMENT, resp.content().get(0).source());
    }
}

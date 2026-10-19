package rw.afriteck.wms.reporting.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rw.afriteck.wms.common.exception.BusinessException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class ReportClockTest {

    private Clock fixedClock;
    private ReportingProperties properties;
    private ReportClock reportClock;

    @BeforeEach
    void setUp() {
        // 2026-05-15T12:00:00Z
        Instant now = Instant.parse("2026-05-15T12:00:00Z");
        fixedClock = Clock.fixed(now, ZoneId.of("UTC"));
        properties = new ReportingProperties("UTC", "RWF", 20000, 30);
        reportClock = new ReportClock(fixedClock, properties);
    }

    @Test
    void nowAndToday() {
        assertEquals(Instant.parse("2026-05-15T12:00:00Z"), reportClock.now());
        assertEquals(LocalDate.parse("2026-05-15"), reportClock.today());
        assertEquals(Instant.parse("2026-05-15T00:00:00Z"), reportClock.startOfDay(LocalDate.parse("2026-05-15")));
    }

    @Test
    void range_valid() {
        LocalDate from = LocalDate.parse("2026-05-01");
        LocalDate to = LocalDate.parse("2026-05-10");
        ReportClock.DateRange range = reportClock.range(from, to);

        assertEquals(Instant.parse("2026-05-01T00:00:00Z"), range.from());
        assertEquals(Instant.parse("2026-05-10T00:00:00Z"), range.to());
    }

    @Test
    void range_nulls() {
        ReportClock.DateRange range = reportClock.range(null, null);
        assertNull(range.from());
        assertNull(range.to());
    }

    @Test
    void range_invalidThrowsException() {
        LocalDate from = LocalDate.parse("2026-05-10");
        LocalDate to = LocalDate.parse("2026-05-01");

        BusinessException ex = assertThrows(BusinessException.class, () -> reportClock.range(from, to));
        assertEquals("INVALID_DATE_RANGE", ex.getCode());
    }

    @Test
    void dayRange_defaults() {
        ReportClock.DayRange range = reportClock.dayRange(null, null, 7);
        // today is 2026-05-15, to defaults to today + 1 = 2026-05-16, from defaults to to - 7 = 2026-05-09
        assertEquals(LocalDate.parse("2026-05-09"), range.from());
        assertEquals(LocalDate.parse("2026-05-16"), range.to());
    }

    @Test
    void dayRange_invalidThrowsException() {
        LocalDate from = LocalDate.parse("2026-05-15");
        LocalDate to = LocalDate.parse("2026-05-10");

        BusinessException ex = assertThrows(BusinessException.class, () -> reportClock.dayRange(from, to, 7));
        assertEquals("INVALID_DATE_RANGE", ex.getCode());
    }
}

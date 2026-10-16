package rw.afriteck.wms.reporting.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.BusinessException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ReportClock {
    private final Clock clock;
    private final ReportingProperties props;

    public record DateRange(Instant from, Instant to) {}
    public record DayRange(LocalDate from, LocalDate to) {}

    public Instant now() { return Instant.now(clock); }
    public LocalDate today() { return LocalDate.now(clock.withZone(props.zoneId())); }
    public Instant startOfDay(LocalDate d) { return d.atStartOfDay(props.zoneId()).toInstant(); }

    /** Both optional. from inclusive, to exclusive (start of that day), in the reporting zone. */
    public DateRange range(LocalDate from, LocalDate to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BusinessException("INVALID_DATE_RANGE", "'from' must be before 'to' ('to' is exclusive)");
        }
        return new DateRange(from == null ? null : startOfDay(from), to == null ? null : startOfDay(to));
    }

    /** Date-based cohort. Defaults: to = tomorrow (so today is included), from = to - defaultDays. */
    public DayRange dayRange(LocalDate from, LocalDate to, int defaultDays) {
        LocalDate end = to != null ? to : today().plusDays(1);
        LocalDate start = from != null ? from : end.minusDays(defaultDays);
        if (!start.isBefore(end)) {
            throw new BusinessException("INVALID_DATE_RANGE", "'from' must be before 'to' ('to' is exclusive)");
        }
        return new DayRange(start, end);
    }
}

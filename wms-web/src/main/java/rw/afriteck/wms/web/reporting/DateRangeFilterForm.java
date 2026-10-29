package rw.afriteck.wms.web.reporting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate; /** Date range shared by the dated reports (supplier performance now, outbound and accuracy reports later). */
@Getter @Setter
public class DateRangeFilterForm {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate to;     // inclusive in the UI

    /** The service takes an EXCLUSIVE end date. */
    public LocalDate toExclusive() { return to == null ? null : to.plusDays(1); }
}

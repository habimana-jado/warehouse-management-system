package rw.afriteck.wms.web.reporting;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/** Formatting helpers for the report screens (bean "repFormat"). Unknown values are an en dash, never 0. */
@Component("repFormat")
public class ReportFormat {

    private static final String DASH = "\u2013";
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ENGLISH);

    public String qty(BigDecimal v)     { return v == null ? DASH : new DecimalFormat("#,##0.####", SYMBOLS).format(v); }
    public String money(BigDecimal v)   { return v == null ? DASH : new DecimalFormat("#,##0.00##", SYMBOLS).format(v); }
    public String percent(BigDecimal v) { return v == null ? DASH : new DecimalFormat("0.##", SYMBOLS).format(v) + " %"; }
    public String date(LocalDate d)     { return d == null ? DASH : d.toString(); }

    public String hours(long h) { return h < 48 ? h + " h" : (h / 24) + " d"; }

    public String duration(Long seconds) {
        if (seconds == null) return DASH;
        long h = seconds / 3600, m = (seconds % 3600) / 60;
        return h > 0 ? h + " h " + String.format("%02d", m) + " min" : m + " min";
    }
}
package rw.afriteck.wms.web.inbound;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Template helper (bean "inboundFormat"): quantities and money. An unknown amount is shown as a dash, never as 0. */
@Component("inboundFormat")
public class InboundFormat {

    private static final String UNKNOWN = "\u2014";

    public String qty(BigDecimal value) {
        return value == null ? UNKNOWN : format(value, 4);
    }

    public String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return UNKNOWN;
        }
        String text = format(amount, 2);
        return currency == null ? text : text + " " + currency;
    }

    private static String format(BigDecimal value, int maxFractionDigits) {
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        df.setMaximumFractionDigits(maxFractionDigits);
        df.setMinimumFractionDigits(0);
        return df.format(value);
    }
}
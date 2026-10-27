package rw.afriteck.wms.web.outbound;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formatting for quantities and money on the outbound screens. An unknown amount is a dash, never 0.
 */
@Component("outboundFormat")
public class OutboundFormat {

    private static final String DASH = "\u2014";

    public String qty(BigDecimal v) {
        return v == null ? DASH : v.stripTrailingZeros().toPlainString();
    }

    public String money(BigDecimal amount, String currency) {
        if (amount == null) return DASH;
        DecimalFormat f = new DecimalFormat("#,##0.####", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return f.format(amount) + " " + currency;
    }
}

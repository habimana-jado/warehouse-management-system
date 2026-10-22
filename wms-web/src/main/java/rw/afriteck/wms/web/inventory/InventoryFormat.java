package rw.afriteck.wms.web.inventory;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component("invFormat")
public class InventoryFormat {

    public String qty(BigDecimal q) {
        return q == null ? "" : q.stripTrailingZeros().toPlainString();
    }

    public String signedQty(BigDecimal q) {
        return q == null ? "" : (q.signum() > 0 ? "+" : "") + qty(q);
    }

    public String date(LocalDate d) {
        return d == null ? "" : d.toString();
    }
}

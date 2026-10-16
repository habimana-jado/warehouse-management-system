package rw.afriteck.wms.reporting.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Rates {
    private Rates() {}
    /** num / den as a percentage with 2 decimals; null when den is null or zero (unknown, never 0). */
    public static BigDecimal percent(BigDecimal num, BigDecimal den) {
        if (num == null || den == null || den.signum() == 0) return null;
        return num.multiply(BigDecimal.valueOf(100)).divide(den, 2, RoundingMode.HALF_UP);
    }
    public static BigDecimal percent(long num, long den) {
        return percent(BigDecimal.valueOf(num), BigDecimal.valueOf(den));
    }
}

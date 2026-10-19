package rw.afriteck.wms.reporting.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RatesTest {

    @Test
    void percent_bigDecimal() {
        assertNull(Rates.percent((BigDecimal) null, BigDecimal.TEN));
        assertNull(Rates.percent(BigDecimal.TEN, null));
        assertNull(Rates.percent(BigDecimal.TEN, BigDecimal.ZERO));

        // 50 / 200 = 25.00
        assertEquals(new BigDecimal("25.00"), Rates.percent(BigDecimal.valueOf(50), BigDecimal.valueOf(200)));
        // 1 / 3 = 33.33
        assertEquals(new BigDecimal("33.33"), Rates.percent(BigDecimal.ONE, BigDecimal.valueOf(3)));
        // 2 / 3 = 66.67
        assertEquals(new BigDecimal("66.67"), Rates.percent(BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
    }

    @Test
    void percent_long() {
        assertNull(Rates.percent(10L, 0L));
        assertEquals(new BigDecimal("50.00"), Rates.percent(5L, 10L));
    }
}

package rw.afriteck.wms.web.outbound;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stock the picker can take from (STORAGE bins only), shown next to the line.
 */
record StockOption(String locationCode, String lotNumber, LocalDate expiryDate, String serialNumber,
                   BigDecimal quantity) {
}

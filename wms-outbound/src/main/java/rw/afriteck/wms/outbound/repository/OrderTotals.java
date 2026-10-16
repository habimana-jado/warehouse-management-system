package rw.afriteck.wms.outbound.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface OrderTotals {
    UUID getOrderId();
    long getLineCount();
    BigDecimal getOrderedQty();
    BigDecimal getPickedQty();
    BigDecimal getShippedQty();
}

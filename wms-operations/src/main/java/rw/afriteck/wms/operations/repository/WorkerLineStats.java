package rw.afriteck.wms.operations.repository;

import java.math.BigDecimal;
import java.util.UUID;

public interface WorkerLineStats {
    UUID getUserId();
    long getLinesCompleted();
    long getShortLines();
    BigDecimal getTotalQtyDone();
}

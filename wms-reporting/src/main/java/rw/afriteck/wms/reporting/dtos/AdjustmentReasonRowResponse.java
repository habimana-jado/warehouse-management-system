package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;

public record AdjustmentReasonRowResponse(String reasonCode, String reasonName,
                                          long adjustments, long lines, BigDecimal increasedQty, BigDecimal decreasedQty) {}

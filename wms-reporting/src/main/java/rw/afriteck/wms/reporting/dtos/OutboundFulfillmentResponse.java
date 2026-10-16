package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OutboundFulfillmentResponse(UUID warehouseId, LocalDate from, LocalDate toExclusive,
                                          Map<String, Long> ordersByStatus,// cohort: order_date in [from, to), every status
                                          long finishedLines, long fullyShippedLines,// lines of SHIPPED and CLOSED orders
                                          BigDecimal lineFillRate, BigDecimal qtyFillRate,
                                          long datedShippedOrders, long onTimeOrders, BigDecimal onTimeRate,// SHIPPED orders with a required date
                                          long overdueOpenOrders) {}                   // as of today, whatever the range


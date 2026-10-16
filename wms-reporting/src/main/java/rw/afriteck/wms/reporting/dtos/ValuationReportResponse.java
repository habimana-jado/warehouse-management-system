package rw.afriteck.wms.reporting.dtos;

import rw.afriteck.wms.common.dtos.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ValuationReportResponse(UUID warehouseId, String currency, Instant asOf,
                                      BigDecimal totalValue,          // sum of KNOWN values only
                                      int itemsValued, int itemsWithoutCost,
                                      BigDecimal unknownCostQty,      // on-hand quantity that has no known cost
                                      PageResponse<ValuationRowResponse> rows) {}

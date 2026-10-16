package rw.afriteck.wms.reporting.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record SupplierPerformanceRowResponse(UUID supplierId, String supplierCode, String supplierName,
                                             long purchaseOrders, long lines, long linesFulfilled,
                                             BigDecimal lineFillRate,// % of lines with accepted >= ordered (headline)
                                             BigDecimal qtyFillRate,// % of ordered quantity accepted (mixes item units)
                                             BigDecimal rejectRate,// rejected / (accepted + rejected), quantity
                                             long datedOrdersReceived, long onTimeOrders,
                                             BigDecimal onTimeRate) {}            // first receipt on or before expected_date

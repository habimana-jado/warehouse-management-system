package rw.afriteck.wms.reporting.dtos;

import java.time.LocalDate;
import java.util.UUID;

public record OpenPurchaseOrderRowResponse(UUID id, String poNumber, UUID supplierId, String supplierName,
                                           String status, LocalDate orderDate, LocalDate expectedDate,
                                           Long daysLate,// null when not late or no expected date
                                           int lineCount, int fulfilledLineCount) {}


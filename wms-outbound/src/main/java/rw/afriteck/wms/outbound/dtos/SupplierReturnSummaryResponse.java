package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;

import java.time.LocalDate;
import java.util.UUID;

public record SupplierReturnSummaryResponse(UUID id, String returnNumber, UUID supplierId, String supplierName,
                                            UUID warehouseId, SupplierReturnStatus status, LocalDate returnDate, String reason,
                                            String supplierReference, int lineCount) {}

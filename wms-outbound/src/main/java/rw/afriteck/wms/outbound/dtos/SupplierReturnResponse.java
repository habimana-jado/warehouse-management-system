package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierReturnResponse(UUID id, long version, String returnNumber,
                                     UUID supplierId, String supplierName, UUID warehouseId, SupplierReturnStatus status,
                                     LocalDate returnDate, String reason, String supplierReference, String notes, String cancelReason,
                                     Instant postedAt, Instant createdAt, List<SupplierReturnLineResponse> lines) {}

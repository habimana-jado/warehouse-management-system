package rw.afriteck.wms.outbound.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateSupplierReturnRequest(UUID supplierId, UUID warehouseId, LocalDate returnDate, String reason,
                                          String supplierReference, String notes,
                                          List<SupplierReturnLineRequest> lines) {}

package rw.afriteck.wms.reporting.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SupplierPerformanceResponse(UUID warehouseId, LocalDate from, LocalDate toExclusive,
                                          List<SupplierPerformanceRowResponse> rows) {}

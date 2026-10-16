package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.reporting.dtos.OpenPurchaseOrderRowResponse;
import rw.afriteck.wms.reporting.dtos.PutawayBacklogResponse;
import rw.afriteck.wms.reporting.dtos.SupplierPerformanceResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface InboundReportService {
    PageResponse<OpenPurchaseOrderRowResponse> openOrders(UUID warehouseId, boolean overdueOnly, int page, int size);
    SupplierPerformanceResponse supplierPerformance(UUID warehouseId, LocalDate from, LocalDate to);
    PutawayBacklogResponse putawayBacklog(UUID warehouseId, int page, int size);
}
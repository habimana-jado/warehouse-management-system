package rw.afriteck.wms.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.OpenPurchaseOrderRowResponse;
import rw.afriteck.wms.reporting.dtos.PutawayBacklogResponse;
import rw.afriteck.wms.reporting.dtos.SupplierPerformanceResponse;
import rw.afriteck.wms.reporting.service.InboundReportService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/inbound")
@RequiredArgsConstructor
class InboundReportController {

    private final InboundReportService service;
    private final UserContext userContext;

    @GetMapping("/open-orders")
    @PreAuthorize(ReportingAccess.BOARD)
    PageResponse<OpenPurchaseOrderRowResponse> openOrders(@RequestParam UUID warehouseId,
                                                          @RequestParam(defaultValue = "false") boolean overdueOnly,
                                                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.openOrders(warehouseId, overdueOnly, page, size);
    }

    @GetMapping("/supplier-performance")
    @PreAuthorize(ReportingAccess.BOARD)
    SupplierPerformanceResponse supplierPerformance(@RequestParam UUID warehouseId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.supplierPerformance(warehouseId, from, to);
    }

    @GetMapping("/putaway-backlog")
    @PreAuthorize(ReportingAccess.BOARD)
    PutawayBacklogResponse putawayBacklog(@RequestParam UUID warehouseId,
                                          @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.putawayBacklog(warehouseId, page, size);
    }
}
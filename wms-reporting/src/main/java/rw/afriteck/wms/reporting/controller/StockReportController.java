package rw.afriteck.wms.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.reporting.dtos.ExpiryRowResponse;
import rw.afriteck.wms.reporting.dtos.StockSummaryResponse;
import rw.afriteck.wms.reporting.dtos.ValuationReportResponse;
import rw.afriteck.wms.reporting.service.ExpiryReportService;
import rw.afriteck.wms.reporting.service.LowStockReportService;
import rw.afriteck.wms.reporting.service.StockSummaryService;
import rw.afriteck.wms.reporting.service.ValuationReportService;
import rw.afriteck.wms.reporting.support.ReportClock;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/stock")
@RequiredArgsConstructor
class StockReportController {

    private final StockSummaryService summaryService;
    private final ExpiryReportService expiryService;
    private final ValuationReportService valuationService;
    private final LowStockReportService lowStockService;
    private final UserContext userContext;
    private final ReportClock clock;

    @GetMapping("/summary")
    @PreAuthorize(ReportingAccess.BOARD)
    PageResponse<StockSummaryResponse> summary(@RequestParam UUID warehouseId,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return summaryService.summary(warehouseId, q, page, size);
    }

    @GetMapping("/expiry")
    @PreAuthorize(ReportingAccess.BOARD)
    PageResponse<ExpiryRowResponse> expiry(@RequestParam UUID warehouseId,
                                           @RequestParam(required = false) Integer days,
                                           @RequestParam(defaultValue = "true") boolean includeExpired,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return expiryService.expiry(warehouseId, days, includeExpired, page, size);
    }

    @GetMapping("/valuation")
    @PreAuthorize(ReportingAccess.BOARD)
    ValuationReportResponse valuation(@RequestParam UUID warehouseId,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return valuationService.valuation(warehouseId, q, page, size);
    }

    @GetMapping("/low-stock")
    @PreAuthorize(ReportingAccess.BOARD)
    List<LowStockResponse> lowStock(@RequestParam UUID warehouseId) {
        userContext.requireWarehouseAccess(warehouseId);
        return lowStockService.list(warehouseId);
    }

    @GetMapping("/low-stock/export")
    @PreAuthorize(ReportingAccess.BOARD)
    ResponseEntity<byte[]> lowStockCsv(@RequestParam UUID warehouseId) {
        userContext.requireWarehouseAccess(warehouseId);
        return Csv.download("low-stock-" + clock.today() + ".csv", lowStockService.csv(warehouseId));
    }
}
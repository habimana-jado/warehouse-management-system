package rw.afriteck.wms.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.AdjustmentsReportResponse;
import rw.afriteck.wms.reporting.dtos.CountAccuracyResponse;
import rw.afriteck.wms.reporting.service.AccuracyReportService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/accuracy")
@RequiredArgsConstructor
class AccuracyReportController {

    private final AccuracyReportService service;
    private final UserContext userContext;

    @GetMapping("/counts")
    @PreAuthorize(ReportingAccess.BOARD)
    CountAccuracyResponse counts(@RequestParam UUID warehouseId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(defaultValue = "10") int topItems) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.countAccuracy(warehouseId, from, to, topItems);
    }

    @GetMapping("/adjustments")
    @PreAuthorize(ReportingAccess.BOARD)
    AdjustmentsReportResponse adjustments(@RequestParam UUID warehouseId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.adjustments(warehouseId, from, to);
    }
}


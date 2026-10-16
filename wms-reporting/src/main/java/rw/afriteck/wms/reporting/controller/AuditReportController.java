package rw.afriteck.wms.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.AuditFeedResponse;
import rw.afriteck.wms.reporting.enums.AuditSource;
import rw.afriteck.wms.reporting.service.AuditReportService;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/audit")
@RequiredArgsConstructor
class AuditReportController {

    private final AuditReportService service;
    private final UserContext userContext;

    @GetMapping
    @PreAuthorize(ReportingAccess.MANAGER)
    AuditFeedResponse feed(@RequestParam UUID warehouseId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(required = false) Set<AuditSource> sources,
                           @RequestParam(required = false) UUID actorId,
                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.feed(warehouseId, from, to, sources, actorId, page, size);
    }
}

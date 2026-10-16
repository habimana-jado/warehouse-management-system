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
import rw.afriteck.wms.reporting.dtos.OpenDemandRowResponse;
import rw.afriteck.wms.reporting.dtos.OutboundFulfillmentResponse;
import rw.afriteck.wms.reporting.dtos.PickingReportResponse;
import rw.afriteck.wms.reporting.dtos.ShipmentsReportResponse;
import rw.afriteck.wms.reporting.service.OutboundReportService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/outbound")
@RequiredArgsConstructor
class OutboundReportController {

    private final OutboundReportService service;
    private final UserContext userContext;

    @GetMapping("/fulfillment")
    @PreAuthorize(ReportingAccess.BOARD)
    OutboundFulfillmentResponse fulfillment(@RequestParam UUID warehouseId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.fulfillment(warehouseId, from, to);
    }

    @GetMapping("/picking")
    @PreAuthorize(ReportingAccess.BOARD)
    PickingReportResponse picking(@RequestParam UUID warehouseId,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.picking(warehouseId, from, to);
    }

    @GetMapping("/shipments")
    @PreAuthorize(ReportingAccess.BOARD)
    ShipmentsReportResponse shipments(@RequestParam UUID warehouseId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.shipments(warehouseId, from, to);
    }

    @GetMapping("/open-demand")
    @PreAuthorize(ReportingAccess.BOARD)
    PageResponse<OpenDemandRowResponse> openDemand(@RequestParam UUID warehouseId,
                                                   @RequestParam(defaultValue = "false") boolean shortfallOnly,
                                                   @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.openDemand(warehouseId, shortfallOnly, page, size);
    }
}
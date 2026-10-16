package rw.afriteck.wms.reporting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.reporting.service.StockLedgerReportService;
import rw.afriteck.wms.reporting.support.ReportClock;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/reports/stock/ledger")
@RequiredArgsConstructor
class StockLedgerReportController {

    private final StockLedgerReportService ledgerService;
    private final UserContext userContext;
    private final ReportClock clock;

    @GetMapping("/export")
    @PreAuthorize(ReportingAccess.BOARD)
    ResponseEntity<byte[]> export(@RequestParam UUID warehouseId,
                                  @RequestParam(required = false) UUID itemId,
                                  @RequestParam(required = false) UUID locationId,
                                  @RequestParam(required = false) UUID lotId,
                                  @RequestParam(required = false) String serialNumber,
                                  @RequestParam(required = false) Set<MovementType> types,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) String referenceType,
                                  @RequestParam(required = false) UUID referenceId) {
        userContext.requireWarehouseAccess(warehouseId);
        ReportClock.DateRange range = clock.range(from, to);
        MovementFilter filter = new MovementFilter(warehouseId, itemId, locationId, lotId,
                Codes.blankToNull(serialNumber) == null ? null : Codes.normalize(serialNumber),
                types == null || types.isEmpty() ? null : types,
                range.from(), range.to(), Codes.blankToNull(referenceType), referenceId);
        return Csv.download("stock-ledger-" + clock.today() + ".csv", ledgerService.exportCsv(filter));
    }
}


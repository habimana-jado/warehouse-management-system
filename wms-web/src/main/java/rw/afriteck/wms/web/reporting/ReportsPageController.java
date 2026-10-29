package rw.afriteck.wms.web.reporting;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.reporting.service.LowStockReportService;
import rw.afriteck.wms.reporting.service.StockLedgerReportService;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Controller
@PreAuthorize(WebAccess.BOARD)
@RequiredArgsConstructor
class ReportsPageController {

    private final UserContext userContext;
    private final WarehouseContext warehouseContext;
    private final LowStockReportService lowStockService;
    private final StockLedgerReportService ledgerService;
    private final ReportClock clock;

    @ModelAttribute("movementTypes")
    MovementType[] movementTypes() { return MovementType.values(); }

    @GetMapping("/reports")
    String index(Model model) {
        boolean manager = userContext.hasRole("ADMIN") || userContext.hasRole("WAREHOUSE_MANAGER");
        model.addAttribute("groups", ReportCatalog.visible(manager));
        return "reporting/index";
    }

    @GetMapping("/reports/low-stock/export")
    ResponseEntity<byte[]> lowStockExport() {
        UUID warehouseId = warehouseContext.require();
        return csv("low-stock-" + clock.today() + ".csv", lowStockService.csv(warehouseId));
    }

    /** A BusinessException (EXPORT_TOO_LARGE, INVALID_DATE_RANGE) is shown by WebExceptionHandler as an error page. */
    @GetMapping("/reports/ledger/export")
    ResponseEntity<byte[]> ledgerExport(@ModelAttribute LedgerExportForm form) {
        UUID warehouseId = warehouseContext.require();
        ReportClock.DateRange range = clock.range(form.getFrom(),
                form.getTo() == null ? null : form.getTo().plusDays(1));          // UI "to" is inclusive
        Set<MovementType> types = form.getType() == null ? null : Set.of(form.getType());
        MovementFilter filter = new MovementFilter(warehouseId, null, null, null, null, types,
                range.from(), range.to(), null, null);
        return csv("stock-ledger-" + clock.today() + ".csv", ledgerService.exportCsv(filter));
    }

    private static ResponseEntity<byte[]> csv(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}

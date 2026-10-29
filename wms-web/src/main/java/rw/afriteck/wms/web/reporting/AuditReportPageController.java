package rw.afriteck.wms.web.reporting;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.reporting.dtos.AuditFeedResponse;
import rw.afriteck.wms.reporting.enums.AuditSource;
import rw.afriteck.wms.reporting.service.AuditReportService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.AuditLinks;
import rw.afriteck.wms.web.support.AuditRow;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.List;
import java.util.UUID;

@Controller
@PreAuthorize(WebAccess.MANAGER)
@RequiredArgsConstructor
class AuditReportPageController {

    private final AuditReportService service;
    private final WorkerLookup workerLookup;
    private final WarehouseContext warehouseContext;

    @ModelAttribute("auditSources")
    AuditSource[] auditSources() { return AuditSource.values(); }

    @GetMapping("/reports/audit")
    String audit(@ModelAttribute("filter") AuditFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        model.addAttribute("workers", workerLookup.findAssignableWorkers(warehouseId));

        AuditFeedResponse feed;
        try {
            feed = service.feed(warehouseId, filter.getFrom(), filter.toExclusive(), filter.getSources(),
                    filter.getActorId(), filter.getPage(), filter.getSize());
        } catch (BusinessException e) {                     // INVALID_DATE_RANGE, DATE_RANGE_TOO_LARGE (31 days)
            model.addAttribute("error", e.getMessage());
            feed = new AuditFeedResponse(List.of(), 0, filter.getSize(), false);
        }
        model.addAttribute("feed", feed);
        model.addAttribute("rows", feed.content().stream()
                .map(e -> new AuditRow(e, AuditLinks.pathFor(e.referenceType(), e.referenceId())))
                .toList());
        return Htmx.isFragmentRequest(request) ? "reporting/audit :: results" : "reporting/audit";
    }
}

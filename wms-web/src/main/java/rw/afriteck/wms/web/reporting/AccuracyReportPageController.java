package rw.afriteck.wms.web.reporting;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.reporting.dtos.AdjustmentsReportResponse;
import rw.afriteck.wms.reporting.dtos.CountAccuracyResponse;
import rw.afriteck.wms.reporting.service.AccuracyReportService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@PreAuthorize(WebAccess.BOARD)
@RequiredArgsConstructor
class AccuracyReportPageController {

    private final AccuracyReportService service;
    private final WarehouseContext warehouseContext;

    @ModelAttribute("topItemChoices")
    List<Integer> topItemChoices() { return List.of(5, 10, 20, 50); }

    @GetMapping("/reports/accuracy/counts")
    String counts(@ModelAttribute("filter") TopItemsFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        CountAccuracyResponse report;
        try {
            report = service.countAccuracy(warehouseId, filter.getFrom(), filter.toExclusive(), filter.getTopItems());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            report = new CountAccuracyResponse(warehouseId, null, null, 0, 0, 0, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of());
        }
        model.addAttribute("report", report);
        return view(request, "reporting/count-accuracy");
    }

    @GetMapping("/reports/accuracy/adjustments")
    String adjustments(@ModelAttribute("filter") DateRangeFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        AdjustmentsReportResponse report;
        try {
            report = service.adjustments(warehouseId, filter.getFrom(), filter.toExclusive());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            report = new AdjustmentsReportResponse(warehouseId, null, null, 0, 0, null, List.of());
        }
        model.addAttribute("report", report);
        return view(request, "reporting/adjustments-report");
    }

    private static String view(HttpServletRequest request, String template) {
        return Htmx.isFragmentRequest(request) ? template + " :: results" : template;
    }
}

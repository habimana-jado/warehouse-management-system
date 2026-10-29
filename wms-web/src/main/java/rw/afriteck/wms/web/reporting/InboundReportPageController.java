package rw.afriteck.wms.web.reporting;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.reporting.dtos.OpenPurchaseOrderRowResponse;
import rw.afriteck.wms.reporting.dtos.PutawayBacklogResponse;
import rw.afriteck.wms.reporting.dtos.SupplierPerformanceResponse;
import rw.afriteck.wms.reporting.service.InboundReportService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Controller
@PreAuthorize(WebAccess.BOARD)
@RequiredArgsConstructor
class InboundReportPageController {

    private final InboundReportService service;
    private final WarehouseContext warehouseContext;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() { return List.of(15, 25, 50, 100); }

    @GetMapping("/reports/inbound/open-orders")
    String openOrders(@ModelAttribute("filter") OpenOrdersFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<OpenPurchaseOrderRowResponse> result =
                service.openOrders(warehouseId, filter.isOverdueOnly(), filter.getPage(), filter.getSize());
        model.addAttribute("page", PageView.of(result, Function.identity()));
        return Htmx.isFragmentRequest(request) ? "reporting/open-orders :: results" : "reporting/open-orders";
    }

    @GetMapping("/reports/inbound/supplier-performance")
    String supplierPerformance(@ModelAttribute("filter") DateRangeFilterForm filter, Model model,
                               HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        SupplierPerformanceResponse report;
        try {
            report = service.supplierPerformance(warehouseId, filter.getFrom(), filter.toExclusive());
        } catch (BusinessException e) {                     // INVALID_DATE_RANGE: alert inside the results fragment
            model.addAttribute("error", e.getMessage());
            report = new SupplierPerformanceResponse(warehouseId, null, null, List.of());
        }
        model.addAttribute("report", report);
        return Htmx.isFragmentRequest(request)
                ? "reporting/supplier-performance :: results" : "reporting/supplier-performance";
    }

    @GetMapping("/reports/inbound/putaway-backlog")
    String putawayBacklog(@ModelAttribute("filter") ReportPageForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PutawayBacklogResponse report = service.putawayBacklog(warehouseId, filter.getPage(), filter.getSize());
        model.addAttribute("report", report);
        model.addAttribute("page", PageView.of(report.rows(), Function.identity()));
        return Htmx.isFragmentRequest(request)
                ? "reporting/putaway-backlog :: results" : "reporting/putaway-backlog";
    }
}

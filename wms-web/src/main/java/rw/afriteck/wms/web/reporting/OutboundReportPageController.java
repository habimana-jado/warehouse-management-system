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
import rw.afriteck.wms.reporting.dtos.OpenDemandRowResponse;
import rw.afriteck.wms.reporting.dtos.OutboundFulfillmentResponse;
import rw.afriteck.wms.reporting.dtos.PickingReportResponse;
import rw.afriteck.wms.reporting.dtos.ShipmentsReportResponse;
import rw.afriteck.wms.reporting.service.OutboundReportService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Controller
@PreAuthorize(WebAccess.BOARD)
@RequiredArgsConstructor
class OutboundReportPageController {

    private final OutboundReportService service;
    private final WarehouseContext warehouseContext;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() { return List.of(15, 25, 50, 100); }

    @GetMapping("/reports/outbound/fulfillment")
    String fulfillment(@ModelAttribute("filter") DateRangeFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        OutboundFulfillmentResponse report;
        try {
            report = service.fulfillment(warehouseId, filter.getFrom(), filter.toExclusive());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            report = new OutboundFulfillmentResponse(warehouseId, null, null, Map.of(), 0, 0, null, null, 0, 0, null, 0);
        }
        model.addAttribute("report", report);
        return view(request, "reporting/fulfillment");
    }

    @GetMapping("/reports/outbound/picking")
    String picking(@ModelAttribute("filter") DateRangeFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PickingReportResponse report;
        try {
            report = service.picking(warehouseId, filter.getFrom(), filter.toExclusive());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            report = new PickingReportResponse(warehouseId, null, null, 0, null, 0, 0, null,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        model.addAttribute("report", report);
        return view(request, "reporting/picking");
    }

    @GetMapping("/reports/outbound/shipments")
    String shipments(@ModelAttribute("filter") DateRangeFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        ShipmentsReportResponse report;
        try {
            report = service.shipments(warehouseId, filter.getFrom(), filter.toExclusive());
        } catch (BusinessException e) {                     // INVALID_DATE_RANGE, DATE_RANGE_TOO_LARGE (366 days)
            model.addAttribute("error", e.getMessage());
            report = new ShipmentsReportResponse(warehouseId, null, null, 0, 0, 0, List.of());
        }
        model.addAttribute("report", report);
        return view(request, "reporting/shipments-report");
    }

    @GetMapping("/reports/outbound/open-demand")
    String openDemand(@ModelAttribute("filter") OpenDemandFilterForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<OpenDemandRowResponse> result =
                service.openDemand(warehouseId, filter.isShortfallOnly(), filter.getPage(), filter.getSize());
        model.addAttribute("page", PageView.of(result, Function.identity()));
        return view(request, "reporting/open-demand");
    }

    private static String view(HttpServletRequest request, String template) {
        return Htmx.isFragmentRequest(request) ? template + " :: results" : template;
    }
}

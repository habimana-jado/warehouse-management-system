package rw.afriteck.wms.web.outbound;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.ShipmentSummaryResponse;
import rw.afriteck.wms.outbound.service.ShipmentService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/shipments")
@PreAuthorize(WebAccess.BOARD)
@RequiredArgsConstructor
class ShipmentPageController {

    private static final List<String> SORT_FIELDS = List.of("shippedAt", "shipmentNumber", "status");   // match ShipmentServiceImpl

    private final ShipmentService shipments;
    private final WarehouseContext warehouseContext;
    private final UiLinks uiLinks;
    private final ShipmentViews views;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return List.of(15, 25, 50, 100);
    }

    @GetMapping
    String list(@ModelAttribute("filter") ShipmentFilterForm filter,
                @PageableDefault(size = 15, sort = "shippedAt", direction = Sort.Direction.DESC) Pageable pageable,
                Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<ShipmentSummaryResponse> response =
                shipments.search(warehouseId, Codes.blankToNull(filter.getQ()), pageable);
        model.addAttribute("page", PageView.of(response, s -> s));
        Map<String, String> urls = new LinkedHashMap<>();
        Map<String, String> classes = new LinkedHashMap<>();
        for (String field : SORT_FIELDS) {
            urls.put(field, uiLinks.sort(field));
            classes.put(field, uiLinks.sortClass(field));
        }
        model.addAttribute("sortUrl", urls);
        model.addAttribute("sortClass", classes);
        return Htmx.isFragmentRequest(request) ? "outbound/shipments :: results" : "outbound/shipments";
    }

    @GetMapping("/{id}")
    String detail(@PathVariable UUID id, Model model, HttpServletRequest request) {
        views.fill(model, id);
        return Htmx.isFragmentRequest(request) ? "outbound/shipment-detail :: panel" : "outbound/shipment-detail";
    }
}


package rw.afriteck.wms.web.inbound;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.CustomerReturnSummaryResponse;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.service.CustomerReturnService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/customer-returns")
@RequiredArgsConstructor
class CustomerReturnPageController {

    /** Must match the whitelist of CustomerReturnServiceImpl. */
    private static final List<String> SORT_FIELDS = List.of("returnNumber", "status", "createdAt", "receivedAt");

    private final CustomerReturnService returns;
    private final CustomerReturnViews views;
    private final WarehouseContext warehouseContext;
    private final UiLinks uiLinks;

    @ModelAttribute("statuses")
    List<CustomerReturnStatus> statuses() {
        return List.of(CustomerReturnStatus.values());
    }

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return List.of(15, 25, 50, 100);
    }

    @GetMapping
    @PreAuthorize(WebAccess.INBOUND_READ)
    String list(@ModelAttribute("filter") ReturnFilterForm filter, @PageableDefault(size = 15) Pageable pageable,
                Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<CustomerReturnSummaryResponse> response =
                returns.search(warehouseId, boardStatuses(filter.getStatus()), Codes.blankToNull(filter.getQ()), pageable);
        model.addAttribute("page", PageView.of(response, CustomerReturnRow::of));
        addSortLinks(model);
        return Htmx.isFragmentRequest(request) ? "inbound/customer-returns :: results" : "inbound/customer-returns";
    }

    @GetMapping("/{id}")
    @PreAuthorize(WebAccess.INBOUND_READ)
    String detail(@PathVariable UUID id, Model model, HttpServletRequest request) {
        views.fill(model, id);
        return Htmx.isFragmentRequest(request) ? "inbound/customer-return-detail :: panel" : "inbound/customer-return-detail";
    }

    /** blank = expected returns, ALL = no filter (null = every status), otherwise one status; unknown falls back to expected. */
    private static Set<CustomerReturnStatus> boardStatuses(String raw) {
        String value = Codes.blankToNull(raw);
        if (value == null) {
            return EnumSet.of(CustomerReturnStatus.EXPECTED);
        }
        if ("ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return EnumSet.of(CustomerReturnStatus.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return EnumSet.of(CustomerReturnStatus.EXPECTED);
        }
    }

    private void addSortLinks(Model model) {
        Map<String, String> urls = new LinkedHashMap<>();
        Map<String, String> classes = new LinkedHashMap<>();
        for (String field : SORT_FIELDS) {
            urls.put(field, uiLinks.sort(field));
            classes.put(field, uiLinks.sortClass(field));
        }
        model.addAttribute("sortUrl", urls);
        model.addAttribute("sortClass", classes);
    }
}
package rw.afriteck.wms.web.outbound;

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
import rw.afriteck.wms.outbound.dtos.SalesOrderFilter;
import rw.afriteck.wms.outbound.dtos.SalesOrderSummaryResponse;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
class SalesOrderPageController {

    /** Must match the whitelist of SalesOrderServiceImpl. */
    private static final List<String> SORT_FIELDS = List.of("orderNumber", "orderDate", "requiredDate", "status", "createdAt");

    private final SalesOrderService salesOrders;
    private final WarehouseContext warehouseContext;
    private final UiLinks uiLinks;
    private final Clock clock;
    private final SalesOrderViews views;

    @ModelAttribute("statuses")
    List<SalesOrderStatus> statuses() {
        return List.of(SalesOrderStatus.values());
    }

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return List.of(15, 25, 50, 100);
    }

    // ------------------------------------------------------------------ board

    @GetMapping
    @PreAuthorize(WebAccess.BOARD)
    String list(@ModelAttribute("filter") SalesOrderFilterForm filter,
                @PageableDefault(size = 15) Pageable pageable,
                Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();            // also checks access
        SalesOrderFilter f = new SalesOrderFilter(warehouseId, boardStatuses(filter.getStatus()),
                null, null, filter.getOrderDateFrom(), filter.getOrderDateTo(), Codes.blankToNull(filter.getQ()));

        PageResponse<SalesOrderSummaryResponse> response = salesOrders.search(f, pageable);
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("page", PageView.of(response, s -> SalesOrderRow.of(s, today)));
        addSortLinks(model);

        return Htmx.isFragmentRequest(request) ? "outbound/sales-orders :: results" : "outbound/sales-orders";
    }

    // ------------------------------------------------------------------ detail

    @GetMapping("/{id}")
    @PreAuthorize(WebAccess.BOARD)
    String detail(@PathVariable UUID id, Model model, HttpServletRequest request) {
        views.fill(model, id);
        return Htmx.isFragmentRequest(request) ? "outbound/sales-order-detail :: panel" : "outbound/sales-order-detail";
    }

    // ------------------------------------------------------------------ helpers

    /** blank = open orders, ALL = no filter (null), otherwise one status; an unknown value falls back to open orders. */
    private static Set<SalesOrderStatus> boardStatuses(String raw) {
        String value = Codes.blankToNull(raw);
        if (value == null) {
            return SalesOrderStatus.OPEN;
        }
        if ("ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return EnumSet.of(SalesOrderStatus.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return SalesOrderStatus.OPEN;
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

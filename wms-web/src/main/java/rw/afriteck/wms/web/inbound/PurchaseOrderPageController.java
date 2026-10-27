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
import rw.afriteck.wms.inbound.dtos.PurchaseOrderFilter;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderSummaryResponse;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
class PurchaseOrderPageController {

    /** Must match the whitelist of PurchaseOrderServiceImpl. */
    private static final List<String> SORT_FIELDS = List.of("poNumber", "orderDate", "expectedDate", "status", "createdAt");

    private final PurchaseOrderService purchaseOrders;
    private final WarehouseContext warehouseContext;
    private final UiLinks uiLinks;
    private final Clock clock;
    private final PurchaseOrderViews views;

    @ModelAttribute("statuses")
    List<PurchaseOrderStatus> statuses() {
        return List.of(PurchaseOrderStatus.values());
    }

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return List.of(15, 25, 50, 100);
    }

    // ------------------------------------------------------------------ board

    @GetMapping
    @PreAuthorize(WebAccess.INBOUND_READ)
    String list(@ModelAttribute("filter") PurchaseOrderFilterForm filter,
                @PageableDefault(size = 15) Pageable pageable,
                Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();            // also checks access
        PurchaseOrderFilter f = new PurchaseOrderFilter(warehouseId, boardStatuses(filter.getStatus()),
                null, null, filter.getOrderDateFrom(), filter.getOrderDateTo(), Codes.blankToNull(filter.getQ()));

        PageResponse<PurchaseOrderSummaryResponse> response = purchaseOrders.search(f, pageable);
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("page", PageView.of(response, s -> PurchaseOrderRow.of(s, today)));
        addSortLinks(model);

        return Htmx.isFragmentRequest(request) ? "inbound/purchase-orders :: results" : "inbound/purchase-orders";
    }

    // ------------------------------------------------------------------ detail

    @GetMapping("/{id}")
    @PreAuthorize(WebAccess.INBOUND_READ)
    String detail(@PathVariable UUID id, Model model, HttpServletRequest request) {
        views.fill(model, id);
        return Htmx.isFragmentRequest(request) ? "inbound/purchase-order-detail :: panel" : "inbound/purchase-order-detail";
    }

    // ------------------------------------------------------------------ helpers

    /** blank = open orders, ALL = no filter (null), otherwise one status; an unknown value falls back to open orders. */
    private static Set<PurchaseOrderStatus> boardStatuses(String raw) {
        String value = Codes.blankToNull(raw);
        if (value == null) {
            return PurchaseOrderStatus.OPEN;
        }
        if ("ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return EnumSet.of(PurchaseOrderStatus.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return PurchaseOrderStatus.OPEN;
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
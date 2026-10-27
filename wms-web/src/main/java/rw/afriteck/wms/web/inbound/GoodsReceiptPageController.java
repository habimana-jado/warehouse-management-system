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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
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
@RequestMapping("/goods-receipts")
@RequiredArgsConstructor
class GoodsReceiptPageController {

    /** Must match the whitelist of GoodsReceiptServiceImpl. */
    private static final List<String> SORT_FIELDS = List.of("receiptNumber", "receivedAt", "mode");

    private final GoodsReceiptService goodsReceipts;
    private final GoodsReceiptViews views;
    private final UserContext userContext;
    private final WarehouseContext warehouseContext;
    private final UiLinks uiLinks;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return List.of(15, 25, 50, 100);
    }

    // ------------------------------------------------------------------ list and put-away board

    @GetMapping
    @PreAuthorize(WebAccess.INBOUND_READ)
    String list(@ModelAttribute("filter") ReceiptFilterForm filter, @PageableDefault(size = 15) Pageable pageable,
                Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<GoodsReceiptSummaryResponse> response =
                goodsReceipts.search(warehouseId, Codes.blankToNull(filter.getQ()), pageable);
        return render(response, false, model, request);
    }

    @GetMapping("/awaiting-putaway")
    @PreAuthorize(WebAccess.INBOUND_READ)
    String board(@ModelAttribute("filter") ReceiptFilterForm filter, @PageableDefault(size = 15) Pageable pageable,
                 Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        return render(goodsReceipts.awaitingPutaway(warehouseId, pageable), true, model, request);
    }

    private String render(PageResponse<GoodsReceiptSummaryResponse> response, boolean awaiting,
                          Model model, HttpServletRequest request) {
        model.addAttribute("page", PageView.of(response, GoodsReceiptRow::of));
        model.addAttribute("awaiting", awaiting);
        model.addAttribute("menu", awaiting ? "putaway-board" : "goods-receipts");
        addSortLinks(model);
        return Htmx.isFragmentRequest(request) ? "inbound/goods-receipts :: results" : "inbound/goods-receipts";
    }

    // ------------------------------------------------------------------ detail and put-away confirmation

    /** A picker may open the receipt of a put-away task assigned to them (GoodsReceiptViews enforces it). */
    @GetMapping("/{id}")
    @PreAuthorize(WebAccess.INBOUND_READ + " or hasRole('PICKER')")
    String detail(@PathVariable UUID id, Model model, HttpServletRequest request) {
        views.fill(model, id);
        return panelOrPage(request);
    }

    @PostMapping("/{id}/lines/{lineId}/putaway")
    @PreAuthorize(WebAccess.WORKER)
    String confirmPutaway(@PathVariable UUID id, @PathVariable UUID lineId,
                          @ModelAttribute("putawayForm") PutawayForm form, Model model, HttpServletRequest request) {
        userContext.requireWarehouseAccess(goodsReceipts.warehouseIdOf(id));
        boolean privileged = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        try {
            goodsReceipts.confirmPutaway(id, lineId, form.toRequest(), userContext.currentUserId(), privileged);
            model.addAttribute("notice", "inbound.receipt.putaway.done");
        } catch (BusinessException e) {                       // includes ConflictException; a 403 (not your task) goes to error/403
            model.addAttribute("error", e.getMessage());
        }
        views.fill(model, id);
        return panelOrPage(request);
    }

    // ------------------------------------------------------------------ helpers

    private static String panelOrPage(HttpServletRequest request) {
        return Htmx.isFragmentRequest(request) ? "inbound/goods-receipt-detail :: panel" : "inbound/goods-receipt-detail";
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
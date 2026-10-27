package rw.afriteck.wms.web.inbound;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;

import java.util.UUID;

/** Every action re-renders the whole detail panel (the active tab resets, a notice or an error shows inside the panel). */
@Controller
@RequestMapping("/purchase-orders")
@PreAuthorize(WebAccess.MANAGER)
@RequiredArgsConstructor
class PurchaseOrderActionController {

    private final PurchaseOrderService purchaseOrders;
    private final PurchaseOrderViews views;
    private final UserContext userContext;
    private final MessageSource messages;

    @PostMapping("/{id}/confirm")
    String confirm(@PathVariable UUID id, Model model, HttpServletRequest request) {
        return run(id, "inbound.po.confirmed", () -> purchaseOrders.confirm(id, userContext.currentUserId()), model, request);
    }

    @PostMapping("/{id}/close")
    String close(@PathVariable UUID id, Model model, HttpServletRequest request) {
        return run(id, "inbound.po.closed", () -> purchaseOrders.close(id, userContext.currentUserId()), model, request);
    }

    @PostMapping("/{id}/cancel")
    String cancel(@PathVariable UUID id,
                  @Valid @ModelAttribute("cancelForm") CancelPurchaseOrderForm form, BindingResult result,
                  Model model, HttpServletRequest request) {
        requireAccess(id);
        if (!result.hasErrors()) {
            try {
                purchaseOrders.cancel(id, form.getReason());
                model.addAttribute("notice", "inbound.po.cancelled");
                model.addAttribute("cancelForm", new CancelPurchaseOrderForm());
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        views.fill(model, id);                             // keeps the bound cancelForm (and its errors) when it is there
        return panel(request);
    }

    @PostMapping("/{id}/lines/{lineId}/tolerance")
    String tolerance(@PathVariable UUID id, @PathVariable UUID lineId,
                     @Valid @ModelAttribute("toleranceForm") ToleranceForm form, BindingResult result,
                     Model model, HttpServletRequest request) {
        requireAccess(id);
        if (result.hasErrors()) {
            model.addAttribute("error",
                    messages.getMessage("inbound.po.tolerance.invalid", null, LocaleContextHolder.getLocale()));
        } else {
            try {
                purchaseOrders.changeTolerance(id, lineId, form.getTolerancePercent());
                model.addAttribute("notice", "inbound.po.toleranceSaved");
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        views.fill(model, id);
        return panel(request);
    }

    // ------------------------------------------------------------------ helpers

    private String run(UUID id, String noticeKey, PanelAction<?> action, Model model, HttpServletRequest request) {
        requireAccess(id);
        try {
            action.execute();
            model.addAttribute("notice", noticeKey);
        } catch (BusinessException e) {                    // includes ConflictException (409): shown inside the panel
            model.addAttribute("error", e.getMessage());
        }
        views.fill(model, id);
        return panel(request);
    }

    private void requireAccess(UUID id) {
        userContext.requireWarehouseAccess(purchaseOrders.warehouseIdOf(id));
    }

    private static String panel(HttpServletRequest request) {
        return Htmx.isFragmentRequest(request) ? "inbound/purchase-order-detail :: panel" : "inbound/purchase-order-detail";
    }
}
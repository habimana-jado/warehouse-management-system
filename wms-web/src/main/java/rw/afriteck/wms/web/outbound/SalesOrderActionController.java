package rw.afriteck.wms.web.outbound;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.CancelSalesOrderRequest;
import rw.afriteck.wms.outbound.dtos.ConfirmSalesOrderResponse;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;

import java.time.ZoneId;
import java.util.UUID;

@Controller
@RequestMapping("/sales-orders")
@PreAuthorize(WebAccess.MANAGER)
@RequiredArgsConstructor
class SalesOrderActionController {

    private final SalesOrderService salesOrders;
    private final PickListService pickLists;
    private final SalesOrderViews views;
    private final UserContext userContext;

    @Value("${wms.ui.zone:Africa/Kigali}")
    private String zone;

    /** Confirms the order; the stock warnings (advisory only) are shown in the panel. */
    @PostMapping("/{id}/confirm")
    String confirm(@PathVariable UUID id, Model model, HttpServletRequest request) {
        requireAccess(id);
        try {
            ConfirmSalesOrderResponse r = salesOrders.confirm(id);
            model.addAttribute("notice", "so.confirmed");
            model.addAttribute("warnings", r.warnings());
        } catch (BusinessException e) {                    // includes ConflictException (409): shown inside the panel
            model.addAttribute("error", e.getMessage());
        }
        views.fill(model, id);
        return panel(request);
    }

    @PostMapping("/{id}/close")
    String close(@PathVariable UUID id, Model model, HttpServletRequest request) {
        return run(id, "so.closed", () -> salesOrders.close(id), model, request);
    }

    @PostMapping("/{id}/cancel")
    String cancel(@PathVariable UUID id,
                  @Valid @ModelAttribute("cancelForm") CancelSalesOrderForm form, BindingResult result,
                  Model model, HttpServletRequest request) {
        requireAccess(id);
        if (Codes.blankToNull(form.getReason()) == null) {
            result.rejectValue("reason", "so.cancel.reason.required", "Enter a reason");
        }
        if (!result.hasErrors()) {
            try {
                salesOrders.cancel(id, new CancelSalesOrderRequest(form.getReason()));
                model.addAttribute("notice", "so.cancelled");
                model.addAttribute("cancelForm", new CancelSalesOrderForm());
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        views.fill(model, id);                             // keeps the bound cancelForm (and its errors) when it is there
        return panel(request);
    }

    /** Puts everything not yet on a pick list (backorders included) on a new pick list and creates the PICK task. */
    @PostMapping("/{id}/release")
    String release(@PathVariable UUID id,
                   @Valid @ModelAttribute("releaseForm") ReleaseForm form, BindingResult result,
                   Model model, HttpServletRequest request) {
        requireAccess(id);
        if (!result.hasErrors()) {
            try {
                pickLists.release(id, form.toRequest(ZoneId.of(zone)));
                model.addAttribute("notice", "so.released");
                model.addAttribute("releaseForm", new ReleaseForm());
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
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        views.fill(model, id);
        return panel(request);
    }

    private void requireAccess(UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
    }

    private static String panel(HttpServletRequest request) {
        return Htmx.isFragmentRequest(request) ? "outbound/sales-order-detail :: panel" : "outbound/sales-order-detail";
    }
}

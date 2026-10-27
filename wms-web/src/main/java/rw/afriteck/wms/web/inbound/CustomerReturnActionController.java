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
import rw.afriteck.wms.inbound.service.CustomerReturnService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;

import java.util.UUID;

/** Every action re-renders the whole detail panel; a notice or an error shows inside it. */
@Controller
@RequestMapping("/customer-returns")
@RequiredArgsConstructor
class CustomerReturnActionController {

    private final CustomerReturnService returns;
    private final CustomerReturnViews views;
    private final UserContext userContext;
    private final MessageSource messages;

    @PostMapping("/{id}/receive")
    @PreAuthorize(WebAccess.INBOUND_RECEIVE)
    String receive(@PathVariable UUID id,
                   @Valid @ModelAttribute("receiveForm") ReceiveReturnForm form, BindingResult result,
                   Model model, HttpServletRequest request) {
        requireAccess(id);
        if (result.hasErrors()) {
            model.addAttribute("error",
                    messages.getMessage("inbound.return.form.invalid", null, LocaleContextHolder.getLocale()));
        } else {
            try {
                returns.receive(id, form.toRequest(), userContext.currentUserId());
                model.addAttribute("notice", "inbound.return.received");
                model.asMap().remove("receiveForm");               // the screen shows the received lines now
            } catch (BusinessException e) {                        // includes ConflictException: shown inside the panel
                model.addAttribute("error", e.getMessage());
            }
        }
        views.fill(model, id);                                     // keeps the typed form when it is still in the model
        return panel(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(WebAccess.MANAGER)
    String cancel(@PathVariable UUID id,
                  @Valid @ModelAttribute("cancelForm") CancelReturnForm form, BindingResult result,
                  Model model, HttpServletRequest request) {
        requireAccess(id);
        if (!result.hasErrors()) {
            try {
                returns.cancel(id, form.getReason());
                model.addAttribute("notice", "inbound.return.cancelled");
                model.addAttribute("cancelForm", new CancelReturnForm());
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        views.fill(model, id);
        return panel(request);
    }

    private void requireAccess(UUID id) {
        userContext.requireWarehouseAccess(returns.warehouseIdOf(id));
    }

    private static String panel(HttpServletRequest request) {
        return Htmx.isFragmentRequest(request) ? "inbound/customer-return-detail :: panel" : "inbound/customer-return-detail";
    }
}
package rw.afriteck.wms.web.outbound;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.CancelPickListRequest;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Pick confirmation re-renders the whole panel; the add and remove row buttons swap one line card only. */
@Controller
@RequestMapping("/pick-lists/{id}")
@RequiredArgsConstructor
class PickListActionController {

    private final PickListService pickLists;
    private final PickListViews views;
    private final UserContext userContext;

    @PostMapping("/lines/{lineId}/confirm")
    @PreAuthorize(WebAccess.OUTBOUND_PICK)
    String confirm(@PathVariable UUID id, @PathVariable UUID lineId,
                   @ModelAttribute("form") PickConfirmForm form, BindingResult result,
                   Model model, HttpServletRequest request) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        Map<UUID, PickConfirmForm> forms = new HashMap<>();
        Map<UUID, String> errors = new HashMap<>();
        try {
            pickLists.confirmPick(id, lineId, form.toRequest(views.isSerial(id, lineId)),
                    userContext.currentUserId(), views.privileged());
            model.addAttribute("notice", "pick.confirmed");
        } catch (BusinessException e) {                  // includes ConflictException; a 403 goes to error/403
            forms.put(lineId, form);                     // keep what the picker typed
            errors.put(lineId, e.getMessage());
        }
        views.fill(model, id, forms, errors);
        return panel(request);
    }

    @PostMapping("/lines/{lineId}/rows/add")
    @PreAuthorize(WebAccess.OUTBOUND_PICK)
    String addRow(@PathVariable UUID id, @PathVariable UUID lineId,
                  @ModelAttribute("form") PickConfirmForm form, BindingResult result, Model model) {
        form.getAllocations().add(new PickAllocationForm());
        return lineFragment(id, lineId, form, model);
    }

    @PostMapping("/lines/{lineId}/rows/remove")
    @PreAuthorize(WebAccess.OUTBOUND_PICK)
    String removeRow(@PathVariable UUID id, @PathVariable UUID lineId, @RequestParam int index,
                     @ModelAttribute("form") PickConfirmForm form, BindingResult result, Model model) {
        if (form.getAllocations().size() > 1 && index >= 0 && index < form.getAllocations().size()) {
            form.getAllocations().remove(index);
        }
        return lineFragment(id, lineId, form, model);
    }

    @PostMapping("/cancel")
    @PreAuthorize(WebAccess.MANAGER)
    String cancel(@PathVariable UUID id,
                  @Valid @ModelAttribute("cancelForm") CancelPickListForm form, BindingResult result,
                  Model model, HttpServletRequest request) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        if (Codes.blankToNull(form.getReason()) == null) {
            result.rejectValue("reason", "pick.cancel.reason.required", "Enter a reason");
        }
        if (!result.hasErrors()) {
            try {
                pickLists.cancel(id, new CancelPickListRequest(form.getReason()));
                model.addAttribute("notice", "pick.cancelled");
                model.addAttribute("cancelForm", new CancelPickListForm());
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        views.fill(model, id);
        return panel(request);
    }

    private String lineFragment(UUID id, UUID lineId, PickConfirmForm form, Model model) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        views.fill(model, id, Map.of(lineId, form), Map.of());
        model.addAttribute("lv", views.line(model, lineId));
        return "outbound/pick-list-detail :: pickLine";
    }

    private static String panel(HttpServletRequest request) {
        return Htmx.isFragmentRequest(request) ? "outbound/pick-list-detail :: panel" : "outbound/pick-list-detail";
    }
}

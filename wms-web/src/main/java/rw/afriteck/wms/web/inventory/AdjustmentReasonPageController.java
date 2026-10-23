package rw.afriteck.wms.web.inventory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;
import rw.afriteck.wms.inventory.service.AdjustmentReasonService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Controller
@RequiredArgsConstructor
@PreAuthorize(WebAccess.ADMIN)
class AdjustmentReasonPageController {

    private final AdjustmentReasonService reasons;
    private final UiLinks uiLinks;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    @ModelAttribute("directions")
    List<AdjustmentDirection> directions() {
        return List.of(AdjustmentDirection.values());       // INCREASE, DECREASE, BOTH
    }

    @GetMapping("/adjustment-reasons")
    String list(@ModelAttribute("filter") ReasonFilterForm filter, BindingResult binding,
                @PageableDefault(size = 15, sort = "name") Pageable pageable, Model model, HttpServletRequest request) {
        PageResponse<AdjustmentReasonResponse> result = reasons.search(filter.getQ(), filter.isIncludeInactive(), pageable);
        model.addAttribute("page", PageView.of(result, Function.identity()));
        InventoryPages.sortLinks(model, uiLinks, "code", "name");
        return Htmx.isFragmentRequest(request) ? "inventory/adjustment-reasons :: results" : "inventory/adjustment-reasons";
    }

    @GetMapping("/adjustment-reasons/new")
    String newForm(Model model) {
        model.addAttribute("form", new AdjustmentReasonForm());
        model.addAttribute("edit", false);
        return "inventory/adjustment-reason-form";
    }

    @PostMapping("/adjustment-reasons")
    String create(@Valid @ModelAttribute("form") AdjustmentReasonForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                AdjustmentReasonResponse created = reasons.create(form.toCreateRequest());
                redirect.addFlashAttribute("flash", Flash.success("inventory.reason.flash.created", created.code()));
                return "redirect:/adjustment-reasons";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("REASON_FIELDS_REQUIRED", "name"));
            }
        }
        model.addAttribute("edit", false);
        return "inventory/adjustment-reason-form";
    }

    @GetMapping("/adjustment-reasons/{id}/edit")
    String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("form", AdjustmentReasonForm.from(reasons.getById(id)));
        model.addAttribute("edit", true);
        model.addAttribute("reasonId", id);
        return "inventory/adjustment-reason-form";
    }

    @PostMapping("/adjustment-reasons/{id}/update")
    String update(@PathVariable UUID id, @Valid @ModelAttribute("form") AdjustmentReasonForm form,
                  BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                AdjustmentReasonResponse updated = reasons.update(id, form.toUpdateRequest());
                redirect.addFlashAttribute("flash", Flash.success("inventory.reason.flash.updated", updated.code()));
                return "redirect:/adjustment-reasons";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("REASON_FIELDS_REQUIRED", "name"));
            }
        }
        model.addAttribute("edit", true);
        model.addAttribute("reasonId", id);
        return "inventory/adjustment-reason-form";
    }

    /** Row action: activate or deactivate. htmx gets the re-rendered row back. */
    @PostMapping("/adjustment-reasons/{id}/status")
    String status(@PathVariable UUID id, @RequestParam boolean active, HttpServletRequest request,
                  Model model, RedirectAttributes redirect) {
        AdjustmentReasonResponse updated = reasons.update(id, new UpdateAdjustmentReasonRequest(null, null, null, active));
        if (Htmx.isFragmentRequest(request)) {
            model.addAttribute("r", updated);
            return "inventory/adjustment-reasons :: row";
        }
        redirect.addFlashAttribute("flash", Flash.success(
                active ? "inventory.reason.flash.activated" : "inventory.reason.flash.deactivated", updated.code()));
        return "redirect:/adjustment-reasons";
    }
}

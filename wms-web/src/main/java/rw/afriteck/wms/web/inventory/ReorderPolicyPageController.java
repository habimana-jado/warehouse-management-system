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
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;
import rw.afriteck.wms.inventory.service.ReorderPolicyService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Controller
@RequiredArgsConstructor
class ReorderPolicyPageController {

    private static final Map<String, String> ERROR_FIELDS = Map.of(
            "REORDER_QTY_INVALID", "minQty",
            "REORDER_LEVELS_INVALID", "maxQty",
            "ITEM_INACTIVE", "item");

    private final ReorderPolicyService policies;
    private final WarehouseContext warehouseContext;
    private final UserContext userContext;
    private final ItemResolver itemResolver;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    /** Admin and manager may change policies; viewers only read. */
    @ModelAttribute("canEdit")
    boolean canEdit() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    // ================================================================== lists

    @GetMapping("/reorder-policies")
    @PreAuthorize(WebAccess.BOARD)
    String list(@ModelAttribute("filter") ReorderFilterForm filter, BindingResult binding,
                @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        model.addAttribute("page", PageView.of(policies.search(warehouseId, filter.isActiveOnly(), pageable), Function.identity()));
        return Htmx.isFragmentRequest(request) ? "inventory/reorder-policies :: results" : "inventory/reorder-policies";
    }

    @GetMapping("/reorder-policies/low-stock")
    @PreAuthorize(WebAccess.BOARD)
    String lowStock(Model model) {
        model.addAttribute("rows", policies.findBelowMinimum(warehouseContext.require()));
        return "inventory/low-stock";
    }

    // ================================================================== create

    @GetMapping("/reorder-policies/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        model.addAttribute("form", new ReorderPolicyForm());
        model.addAttribute("edit", false);
        return "inventory/reorder-policy-form";
    }

    @PostMapping("/reorder-policies")
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") ReorderPolicyForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        ItemResolver.Resolution match = itemResolver.resolve(form.getItem());
        if (!match.found() && !result.hasFieldErrors("item")) {
            if (match.ambiguous()) {
                result.rejectValue("item", "inventory.item.ambiguous", new Object[]{match.candidateSkus()}, "Several items match");
            } else {
                result.rejectValue("item", "inventory.item.unknown", new Object[]{form.getItem()}, "Unknown item");
            }
        }
        if (!result.hasErrors()) {
            try {
                ReorderPolicyResponse created = policies.create(form.toCreateRequest(warehouseId, match.item().id()));
                redirect.addFlashAttribute("flash", Flash.success("inventory.reorder.flash.created", created.itemSku()));
                return "redirect:/reorder-policies";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        model.addAttribute("edit", false);
        return "inventory/reorder-policy-form";
    }

    // ================================================================== edit

    @GetMapping("/reorder-policies/{id}/edit")
    @PreAuthorize(WebAccess.MANAGER)
    String editForm(@PathVariable UUID id, Model model) {
        userContext.requireWarehouseAccess(policies.warehouseIdOf(id));
        ReorderPolicyResponse policy = policies.getById(id);
        model.addAttribute("form", ReorderPolicyForm.from(policy));
        model.addAttribute("policy", policy);
        model.addAttribute("policyId", id);
        model.addAttribute("edit", true);
        return "inventory/reorder-policy-form";
    }

    @PostMapping("/reorder-policies/{id}/update")
    @PreAuthorize(WebAccess.MANAGER)
    String update(@PathVariable UUID id, @Valid @ModelAttribute("form") ReorderPolicyForm form,
                  BindingResult result, Model model, RedirectAttributes redirect) {
        userContext.requireWarehouseAccess(policies.warehouseIdOf(id));
        if (!result.hasErrors()) {
            try {
                ReorderPolicyResponse updated = policies.update(id, form.toUpdateRequest());
                redirect.addFlashAttribute("flash", Flash.success("inventory.reorder.flash.updated", updated.itemSku()));
                return "redirect:/reorder-policies";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        model.addAttribute("policy", policies.getById(id));
        model.addAttribute("policyId", id);
        model.addAttribute("edit", true);
        return "inventory/reorder-policy-form";
    }

    /** Row action: activate or deactivate. */
    @PostMapping("/reorder-policies/{id}/status")
    @PreAuthorize(WebAccess.MANAGER)
    String status(@PathVariable UUID id, @RequestParam boolean active, HttpServletRequest request,
                  Model model, RedirectAttributes redirect) {
        userContext.requireWarehouseAccess(policies.warehouseIdOf(id));
        ReorderPolicyResponse updated = policies.update(id, new UpdateReorderPolicyRequest(null, null, null, null, active));
        if (Htmx.isFragmentRequest(request)) {
            model.addAttribute("p", updated);
            return "inventory/reorder-policies :: row";
        }
        redirect.addFlashAttribute("flash", Flash.success(
                active ? "inventory.reorder.flash.activated" : "inventory.reorder.flash.deactivated", updated.itemSku()));
        return "redirect:/reorder-policies";
    }
}

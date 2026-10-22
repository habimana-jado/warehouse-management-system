package rw.afriteck.wms.web.inventory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inventory.dtos.AdjustmentAccessInfo;
import rw.afriteck.wms.inventory.dtos.AdjustmentFilter;
import rw.afriteck.wms.inventory.dtos.AdjustmentResponse;
import rw.afriteck.wms.inventory.dtos.AdjustmentSummaryResponse;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.service.AdjustmentReasonService;
import rw.afriteck.wms.inventory.service.AdjustmentService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Controller
@RequiredArgsConstructor
class AdjustmentPageController {

    /** Service error code -> form field; every other code becomes a global error on the form. */
    private static final Map<String, String> ERROR_FIELDS = Map.of(
            "REASON_INACTIVE", "reasonId",
            "ADJUSTMENT_DIRECTION_INVALID", "reasonId");

    private final AdjustmentService adjustments;
    private final AdjustmentReasonService reasons;
    private final WarehouseContext warehouseContext;
    private final UserContext userContext;
    private final BinOptions binOptions;
    private final ItemResolver itemResolver;
    private final UiLinks uiLinks;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    @ModelAttribute("statuses")
    List<AdjustmentStatus> statuses() {
        return List.of(AdjustmentStatus.values());
    }

    @ModelAttribute("directions")
    List<AdjustmentDirection> directions() {
        return List.of(AdjustmentDirection.INCREASE, AdjustmentDirection.DECREASE);
    }

    // ================================================================== lists

    @GetMapping("/adjustments")
    @PreAuthorize(WebAccess.BOARD)
    String board(@ModelAttribute("filter") AdjustmentFilterForm filter, BindingResult binding,
                 @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        return list(false, filter, pageable, model, request);
    }

    @GetMapping("/adjustments/my")
    @PreAuthorize(WebAccess.WORKER)
    String mine(@ModelAttribute("filter") AdjustmentFilterForm filter, BindingResult binding,
                @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        return list(true, filter, pageable, model, request);
    }

    private String list(boolean mine, AdjustmentFilterForm filter, Pageable pageable, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        Set<AdjustmentStatus> statuses = filter.getStatus() == null ? Set.of() : EnumSet.of(filter.getStatus());
        UUID requestedBy = mine ? userContext.currentUserId() : null;       // workers only ever see their own
        PageResponse<AdjustmentSummaryResponse> result = adjustments.search(
                new AdjustmentFilter(warehouseId, statuses, filter.getQ(), requestedBy), pageable);

        model.addAttribute("page", PageView.of(result, Function.identity()));
        model.addAttribute("mine", mine);
        model.addAttribute("menu", mine ? "my-adjustments" : "adjustments");
        model.addAttribute("listPath", mine ? "/adjustments/my" : "/adjustments");
        InventoryPages.sortLinks(model, uiLinks, "createdAt", "adjustmentNumber", "status");
        return Htmx.isFragmentRequest(request) ? "inventory/adjustments :: results" : "inventory/adjustments";
    }

    // ================================================================== create

    @GetMapping("/adjustments/new")
    @PreAuthorize(WebAccess.WORKER)
    String newForm(Model model) {
        AdjustmentForm form = new AdjustmentForm();
        form.getLines().add(new AdjustmentLineForm());
        model.addAttribute("form", form);
        fillForm(model);
        return "inventory/adjustment-form";
    }

    @PostMapping("/adjustments")
    @PreAuthorize(WebAccess.WORKER)
    String create(@Valid @ModelAttribute("form") AdjustmentForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        List<UUID> itemIds = resolveItems(form, result);
        if (!result.hasErrors()) {
            try {
                AdjustmentResponse created = adjustments.create(form.toRequest(warehouseId, itemIds), userContext.currentUserId());
                String key = created.status() == AdjustmentStatus.POSTED
                        ? "inventory.adjustment.flash.posted" : "inventory.adjustment.flash.created";
                redirect.addFlashAttribute("flash", Flash.success(key, created.adjustmentNumber()));
                return "redirect:/adjustments/" + created.id();
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        fillForm(model);
        return "inventory/adjustment-form";
    }

    /** htmx: append an empty line. The whole form is posted, so the typed values survive. */
    @PostMapping("/adjustments/new/lines/add")
    @PreAuthorize(WebAccess.WORKER)
    String addLine(@ModelAttribute("form") AdjustmentForm form, BindingResult ignored, Model model) {
        form.getLines().add(new AdjustmentLineForm());
        fillForm(model);
        return "inventory/adjustment-form :: linesCard";
    }

    @PostMapping("/adjustments/new/lines/remove")
    @PreAuthorize(WebAccess.WORKER)
    String removeLine(@ModelAttribute("form") AdjustmentForm form, BindingResult ignored,
                      @RequestParam int index, Model model) {
        if (form.getLines().size() > 1 && index >= 0 && index < form.getLines().size()) {
            form.getLines().remove(index);
        }
        fillForm(model);
        return "inventory/adjustment-form :: linesCard";
    }

    private void fillForm(Model model) {
        UUID warehouseId = warehouseContext.require();
        model.addAttribute("bins", binOptions.forWarehouse(warehouseId));
        model.addAttribute("reasons", reasons.search(null, false, PageRequest.of(0, 100, Sort.by("name"))).content());
        model.addAttribute("menu", workerOnly() ? "my-adjustments" : "adjustments");
    }

    /** Typed or scanned items become item ids; an unknown or ambiguous entry is a field error on that line. */
    private List<UUID> resolveItems(AdjustmentForm form, BindingResult result) {
        List<UUID> ids = new ArrayList<>(form.getLines().size());
        for (int i = 0; i < form.getLines().size(); i++) {
            String typed = form.getLines().get(i).getItem();
            ItemResolver.Resolution match = itemResolver.resolve(typed);
            ids.add(match.found() ? match.item().id() : null);
            String field = "lines[" + i + "].item";
            if (!match.found() && !result.hasFieldErrors(field)) {
                if (match.ambiguous()) {
                    result.rejectValue(field, "inventory.item.ambiguous", new Object[]{match.candidateSkus()},
                            "Several items match");
                } else {
                    result.rejectValue(field, "inventory.item.unknown", new Object[]{typed}, "Unknown item");
                }
            }
        }
        return ids;
    }

    // ================================================================== detail and actions

    @GetMapping("/adjustments/{id}")
    @PreAuthorize(WebAccess.ANY_USER)
    String detail(@PathVariable UUID id, Model model) {
        requireReadAccess(id);
        fillPanel(model, adjustments.getById(id), new RejectAdjustmentForm());
        return "inventory/adjustment-detail";
    }

    @PostMapping("/adjustments/{id}/approve")
    @PreAuthorize(WebAccess.MANAGER)
    String approve(@PathVariable UUID id, HttpServletRequest request, Model model, RedirectAttributes redirect) {
        return run(id, "inventory.adjustment.flash.approved",
                () -> adjustments.approve(id, userContext.currentUserId()), request, model, redirect);
    }

    @PostMapping("/adjustments/{id}/reject")
    @PreAuthorize(WebAccess.MANAGER)
    String reject(@PathVariable UUID id, @Valid @ModelAttribute("rejectForm") RejectAdjustmentForm form,
                  BindingResult result, HttpServletRequest request, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {                                   // re-render with the field error, status 200
            requireReadAccess(id);
            fillPanel(model, adjustments.getById(id), form);
            return Htmx.isFragmentRequest(request) ? "inventory/adjustment-detail :: panel" : "inventory/adjustment-detail";
        }
        return run(id, "inventory.adjustment.flash.rejected",
                () -> adjustments.reject(id, userContext.currentUserId(), form.getReason()), request, model, redirect);
    }

    @PostMapping("/adjustments/{id}/cancel")
    @PreAuthorize(WebAccess.WORKER)
    String cancel(@PathVariable UUID id, HttpServletRequest request, Model model, RedirectAttributes redirect) {
        return run(id, "inventory.adjustment.flash.cancelled",
                () -> adjustments.cancel(id, userContext.currentUserId()), request, model, redirect);
    }

    /** Runs one action: htmx gets the re-rendered panel, a plain post is redirected (post / redirect / get). */
    private String run(UUID id, String messageKey, Supplier<AdjustmentResponse> action,
                       HttpServletRequest request, Model model, RedirectAttributes redirect) {
        requireReadAccess(id);
        boolean htmx = Htmx.isFragmentRequest(request);
        try {
            AdjustmentResponse updated = action.get();
            if (htmx) {
                fillPanel(model, updated, new RejectAdjustmentForm());
                model.addAttribute("notice", messageKey);
                return "inventory/adjustment-detail :: panel";
            }
            redirect.addFlashAttribute("flash", Flash.success(messageKey, updated.adjustmentNumber()));
            return "redirect:/adjustments/" + id;
        } catch (BusinessException e) {        // e.g. SELF_APPROVAL_NOT_ALLOWED, INSUFFICIENT_STOCK, ADJUSTMENT_INVALID_STATE
            if (htmx) {
                fillPanel(model, adjustments.getById(id), new RejectAdjustmentForm());
                model.addAttribute("error", e.getMessage());
                return "inventory/adjustment-detail :: panel";
            }
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/adjustments/" + id;
        }
    }

    // ================================================================== helpers

    private void fillPanel(Model model, AdjustmentResponse a, RejectAdjustmentForm rejectForm) {
        UUID me = userContext.currentUserId();
        boolean pending = a.status() == AdjustmentStatus.PENDING_APPROVAL;
        boolean own = me.equals(a.requestedBy());
        boolean manager = isManager();
        model.addAttribute("view", new AdjustmentView(a,
                pending && manager && !own,
                pending && manager,
                pending && own,
                pending && manager && own));
        if (!model.containsAttribute("rejectForm")) {               // keep the bound form when it carries errors
            model.addAttribute("rejectForm", rejectForm);
        }
        model.addAttribute("menu", workerOnly() ? "my-adjustments" : "adjustments");
    }

    /** Same rule as the REST controller: warehouse access, and workers only see their own adjustments. */
    private void requireReadAccess(UUID id) {
        AdjustmentAccessInfo info = adjustments.accessInfo(id);
        userContext.requireWarehouseAccess(info.warehouseId());
        if (workerOnly() && !userContext.currentUserId().equals(info.requestedBy())) {
            throw new ForbiddenException("Workers can only view their own adjustments");
        }
    }

    private boolean isManager() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private boolean workerOnly() {
        return !(isManager() || userContext.hasRole("VIEWER"));
    }
}

package rw.afriteck.wms.web.foundation.items;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.dtos.ItemFilter;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponseDetailed;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemCategoryService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.*;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
@PreAuthorize(WebAccess.ANY_USER)                      // reads; the write methods tighten it to MANAGER
public class ItemPageController {

    private static final Set<String> SORTS = Set.of("sku", "name");

    private final ItemService items;
    private final ItemCategoryService categories;
    private final UserContext userContext;
    private final UiLinks uiLinks;

    @ModelAttribute("trackingTypes")
    List<TrackingType> trackingTypes() { return List.of(TrackingType.values()); }

    // ---------- list ----------

    @GetMapping
    String list(@ModelAttribute("filter") ItemFilter filter,
                @PageableDefault(size = 15) Pageable pageable,
                HttpServletRequest request, Model model) {

        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by("sku"));     // adjust to the real signature
        PageResponse<ItemResponse> result = PageResponse.of(items.search(filter, safe));

        model.addAttribute("page", PageView.of(result, i -> i));                   // ItemResponse already has the category name
        model.addAttribute("sorts", uiLinks.sorts("sku", "name"));
        model.addAttribute("categoryOptions", categories.findAll());               // the filter lists inactive ones too
        model.addAttribute("canManage", canManage());
        return Htmx.isFragmentRequest(request) ? "items/list :: results" : "items/list";
    }

    // ---------- create ----------

    @GetMapping("/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        model.addAttribute("form", new ItemCreateForm());
        addCategoryOptions(model, null);
        return "items/create";
    }

    @PostMapping
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") ItemCreateForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        checkUoms(form.getUoms(), result);
        if (!result.hasErrors()) {
            try {
                ItemResponseDetailed created = items.create(form.toRequest());
                redirect.addFlashAttribute("flash", Flash.success("items.created", created.sku()));
                return "redirect:/items";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of(
                        "SKU_EXISTS", "sku",
                        "BARCODE_EXISTS", "barcode",
                        "INVALID_TRACKING", "expiryTracked"));
            }
        }
        addCategoryOptions(model, form.getCategoryId());
        return "items/create";                                                       // status 200 on purpose
    }

    // ---------- edit ----------

    @GetMapping("/{id}/edit")
    @PreAuthorize(WebAccess.MANAGER)
    String edit(@PathVariable UUID id, Model model) {
        ItemResponseDetailed d = items.getDetailedById(id);
        fillEditModel(model, d);
        model.addAttribute("form", ItemEditForm.from(d));
        model.addAttribute("uomsForm", ItemUomsForm.from(d));
        return "items/edit";
    }

    @PostMapping("/{id}/details")
    @PreAuthorize(WebAccess.MANAGER)
    String updateDetails(@PathVariable UUID id, @Valid @ModelAttribute("form") ItemEditForm form,
                         BindingResult result, Model model) {
        ItemResponseDetailed current = items.getDetailedById(id);
        if (!result.hasErrors()) {
            try {
                current = items.update(id, form.toRequest());
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("BARCODE_EXISTS", "barcode"));
            }
        }
        fillEditModel(model, current);
        return "items/edit :: detailsCard";
    }

    @PostMapping("/{id}/uoms")
    @PreAuthorize(WebAccess.MANAGER)
    String updateUoms(@PathVariable UUID id, @Valid @ModelAttribute("uomsForm") ItemUomsForm form,
                      BindingResult result, Model model) {
        ItemResponseDetailed current = items.getDetailedById(id);
        checkUoms(form.getUoms(), result);
        if (!result.hasErrors()) {
            try {
                current = items.replaceUoms(id, form.toRequest());
                model.addAttribute("uomsForm", ItemUomsForm.from(current));        // fresh rows, no duplicates
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of());                              // INVALID_UOM, DUPLICATE_UOM, ...
            }
        }
        model.addAttribute("itemId", id);
        model.addAttribute("item", current);
        return "items/edit :: unitsCard";
    }

    // ---------- row actions (guards, such as stock on hand, reach the user as a toast) ----------

    @PostMapping("/{id}/activate")
    @PreAuthorize(WebAccess.MANAGER)
    String activate(@PathVariable UUID id, Model model) { return row(items.activate(id), model); }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize(WebAccess.MANAGER)
    String deactivate(@PathVariable UUID id, Model model) { return row(items.deactivate(id), model); }

    // ---------- helpers ----------

    private String row(ItemResponseDetailed d, Model model) {
        model.addAttribute("item", new ItemResponse(d.id(), d.sku(), d.name(), d.barcode(),
                d.categoryId(), d.categoryName(), d.baseUom(), d.trackingType(), d.expiryTracked(), d.active()));
        model.addAttribute("canManage", true);
        return "items/list :: row";
    }

    private void fillEditModel(Model model, ItemResponseDetailed d) {
        model.addAttribute("itemId", d.id());
        model.addAttribute("item", d);
        addCategoryOptions(model, d.categoryId());
    }

    /** Active categories, plus the item's current one when it has since been deactivated. */
    private void addCategoryOptions(Model model, UUID currentCategoryId) {
        List<ItemCategoryResponse> options = new ArrayList<>(categories.findAllActive());
        if (currentCategoryId != null && options.stream().noneMatch(c -> c.id().equals(currentCategoryId))) {
            options.add(categories.getById(currentCategoryId));
        }
        model.addAttribute("categoryOptions", options);
    }

    /** A unit row that is partly filled needs both the unit code and the factor. */
    private void checkUoms(List<ItemUomForm> rows, BindingResult result) {
        for (int i = 0; i < rows.size(); i++) {
            ItemUomForm r = rows.get(i);
            if (r.blank()) continue;
            if (!StringUtils.hasText(r.getUom())) {
                result.rejectValue("uoms[" + i + "].uom", "items.uom.required", "Required");
            }
            if (r.getFactorToBase() == null) {
                result.rejectValue("uoms[" + i + "].factorToBase", "items.uom.required", "Required");
            }
        }
    }

    private boolean canManage() {
        return userContext.hasRole("ADMIN") || userContext.hasRole("WAREHOUSE_MANAGER");
    }
}
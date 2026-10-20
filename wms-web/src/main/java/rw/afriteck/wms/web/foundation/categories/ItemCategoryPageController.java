package rw.afriteck.wms.web.foundation.categories;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.service.ItemCategoryService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Flash;
import rw.afriteck.wms.web.support.FormErrors;
import rw.afriteck.wms.web.support.Htmx;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
@PreAuthorize(WebAccess.BOARD)                         // reads; the write methods tighten it to MANAGER
public class ItemCategoryPageController {

    private final ItemCategoryService categories;
    private final UserContext userContext;

    @GetMapping
    String list(@RequestParam(required = false) String q, HttpServletRequest request, Model model) {
        String term = q == null ? "" : q.trim().toLowerCase();
        List<ItemCategoryResponse> all = categories.findAll().stream()
                .filter(c -> term.isEmpty()
                        || c.name().toLowerCase().contains(term)
                        || (c.description() != null && c.description().toLowerCase().contains(term)))
                .toList();

        model.addAttribute("categories", all);
        model.addAttribute("q", q);
        model.addAttribute("canManage", canManage());
        return Htmx.isFragmentRequest(request) ? "categories/list :: results" : "categories/list";
    }

    @GetMapping("/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        model.addAttribute("form", new ItemCategoryForm());
        return "categories/create";
    }

    @PostMapping
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") ItemCategoryForm form, BindingResult result,
                  RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                ItemCategoryResponse created = categories.create(form.toCreateRequest());
                redirect.addFlashAttribute("flash", Flash.success("categories.created", created.name()));
                return "redirect:/categories";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("CATEGORY_EXISTS", "name"));
            }
        }
        return "categories/create";                                          // status 200 on purpose
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize(WebAccess.MANAGER)
    String edit(@PathVariable UUID id, Model model) {
        model.addAttribute("categoryId", id);
        model.addAttribute("form", ItemCategoryForm.from(categories.getById(id)));
        return "categories/edit";
    }

    @PostMapping("/{id}/details")
    @PreAuthorize(WebAccess.MANAGER)
    String updateDetails(@PathVariable UUID id, @Valid @ModelAttribute("form") ItemCategoryForm form,
                         BindingResult result, Model model) {
        model.addAttribute("categoryId", id);
        if (!result.hasErrors()) {
            try {
                categories.update(id, form.toUpdateRequest());
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("CATEGORY_EXISTS", "name"));
            }
        }
        return "categories/edit :: detailsCard";
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize(WebAccess.MANAGER)
    String activate(@PathVariable UUID id, Model model) {
        return row(categories.setActive(id, true), model);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize(WebAccess.MANAGER)
    String deactivate(@PathVariable UUID id, Model model) {
        return row(categories.setActive(id, false), model);
    }

    private String row(ItemCategoryResponse c, Model model) {
        model.addAttribute("cat", c);
        model.addAttribute("canManage", true);
        return "categories/list :: row";
    }

    private boolean canManage() {
        return userContext.hasRole("ADMIN") || userContext.hasRole("WAREHOUSE_MANAGER");
    }
}
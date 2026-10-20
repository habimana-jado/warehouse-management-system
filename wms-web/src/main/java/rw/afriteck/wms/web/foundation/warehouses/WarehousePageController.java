package rw.afriteck.wms.web.foundation.warehouses;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/warehouses")
@RequiredArgsConstructor
@PreAuthorize(WebAccess.ADMIN)
public class WarehousePageController {

    private static final Set<String> SORTS = Set.of("code", "name", "city");

    private final WarehouseService warehouses;
    private final UiLinks uiLinks;

    @GetMapping
    String list(@RequestParam(required = false) String q,
                @PageableDefault(size = 15) Pageable pageable,
                HttpServletRequest request, Model model) {

        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by("name"));   // adjust to the real signature
        PageResponse<WarehouseResponse> result = PageResponse.of(warehouses.search(q, safe));

        model.addAttribute("page", PageView.of(result, w -> w));        // the response already has everything the row needs
        model.addAttribute("q", q);
        model.addAttribute("sorts", uiLinks.sorts("code", "name", "city"));
        return Htmx.isFragmentRequest(request) ? "warehouses/list :: results" : "warehouses/list";
    }

    @GetMapping("/new")
    String newForm(Model model) {
        model.addAttribute("form", new WarehouseCreateForm());
        return "warehouses/create";
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") WarehouseCreateForm form, BindingResult result,
                  RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                WarehouseResponse created = warehouses.create(form.toRequest());
                redirect.addFlashAttribute("flash", Flash.success("warehouses.created", created.code()));
                return "redirect:/warehouses";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("WAREHOUSE_CODE_EXISTS", "code"));
            }
        }
        return "warehouses/create";                                     // status 200 on purpose
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable UUID id, Model model) {
        WarehouseResponse w = warehouses.getById(id);
        model.addAttribute("warehouseId", id);
        model.addAttribute("code", w.code());
        model.addAttribute("form", WarehouseEditForm.from(w));
        return "warehouses/edit";
    }

    @PostMapping("/{id}/details")
    String updateDetails(@PathVariable UUID id,
                         @Valid @ModelAttribute("form") WarehouseEditForm form,
                         BindingResult result, Model model) {
        model.addAttribute("warehouseId", id);
        model.addAttribute("code", warehouses.getById(id).code());
        if (!result.hasErrors()) {
            try {
                warehouses.update(id, form.toRequest());
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of());
            }
        }
        return "warehouses/edit :: detailsCard";
    }

    // row actions in the list: the guards (stock, open tasks) can refuse a deactivation with a
    // ConflictException; WebExceptionHandler turns it into a toast and the row stays as it was
    @PostMapping("/{id}/activate")
    String activate(@PathVariable UUID id, Model model) {
        model.addAttribute("w", warehouses.activate(id));
        return "warehouses/list :: row";
    }

    @PostMapping("/{id}/deactivate")
    String deactivate(@PathVariable UUID id, Model model) {
        model.addAttribute("w", warehouses.deactivate(id));
        return "warehouses/list :: row";
    }
}
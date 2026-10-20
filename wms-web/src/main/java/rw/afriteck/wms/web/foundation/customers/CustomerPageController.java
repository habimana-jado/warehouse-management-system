package rw.afriteck.wms.web.foundation.customers;

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
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
@PreAuthorize(WebAccess.BOARD)
public class CustomerPageController {

    private static final Set<String> SORTS = Set.of("code", "name", "city");

    private final CustomerService customers;
    private final UserContext userContext;
    private final UiLinks uiLinks;

    @GetMapping
    String list(@RequestParam(required = false) String q,
                @PageableDefault(size = 15) Pageable pageable,
                HttpServletRequest request, Model model) {

        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by("name"));        // adjust to the real signature
        PageResponse<CustomerResponse> result = PageResponse.of(customers.search(q, safe));

        model.addAttribute("page", PageView.of(result, s -> s));
        model.addAttribute("q", q);
        model.addAttribute("sorts", uiLinks.sorts("code", "name", "city"));
        model.addAttribute("canManage", canManage());
        return Htmx.isFragmentRequest(request) ? "customers/list :: results" : "customers/list";
    }

    @GetMapping("/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        model.addAttribute("form", new CustomerCreateForm());
        return "customers/create";
    }

    @PostMapping
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") CustomerCreateForm form, BindingResult result,
                  RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                CustomerResponse created = customers.create(form.toRequest());
                redirect.addFlashAttribute("flash", Flash.success("customers.created", created.code()));
                return "redirect:/customers";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("Customer_CODE_EXISTS", "code"));
            }
        }
        return "customers/create";                                                    // status 200 on purpose
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize(WebAccess.MANAGER)
    String edit(@PathVariable UUID id, Model model) {
        CustomerResponse s = customers.getById(id);
        model.addAttribute("customerId", id);
        model.addAttribute("code", s.code());
        model.addAttribute("form", CustomerEditForm.from(s));
        return "customers/edit";
    }

    @PostMapping("/{id}/details")
    @PreAuthorize(WebAccess.MANAGER)
    String updateDetails(@PathVariable UUID id, @Valid @ModelAttribute("form") CustomerEditForm form,
                         BindingResult result, Model model) {
        model.addAttribute("customerId", id);
        model.addAttribute("code", customers.getById(id).code());
        if (!result.hasErrors()) {
            try {
                customers.update(id, form.toRequest());
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of());
            }
        }
        return "customers/edit :: detailsCard";
    }

    // row actions: the outbound guard (open sales orders) refuses a deactivation with a
    // ConflictException; WebExceptionHandler turns it into a toast and the row stays as it was
    @PostMapping("/{id}/activate")
    @PreAuthorize(WebAccess.MANAGER)
    String activate(@PathVariable UUID id, Model model) { return row(customers.activate(id), model); }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize(WebAccess.MANAGER)
    String deactivate(@PathVariable UUID id, Model model) { return row(customers.deactivate(id), model); }

    private String row(CustomerResponse s, Model model) {
        model.addAttribute("s", s);
        model.addAttribute("canManage", true);
        return "suppliers/list :: row";
    }

    private boolean canManage() {
        return userContext.hasRole("ADMIN") || userContext.hasRole("WAREHOUSE_MANAGER");
    }
}
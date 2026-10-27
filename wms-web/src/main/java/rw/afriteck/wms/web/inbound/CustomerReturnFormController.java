package rw.afriteck.wms.web.inbound;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.inbound.dtos.CustomerReturnResponse;
import rw.afriteck.wms.inbound.service.CustomerReturnService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.inventory.ItemResolver;
import rw.afriteck.wms.web.support.Flash;
import rw.afriteck.wms.web.support.FormErrors;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/customer-returns")
@PreAuthorize(WebAccess.MANAGER)
@RequiredArgsConstructor
class CustomerReturnFormController {

    private static final String FORM_VIEW = "inbound/customer-return-form";
    private static final String LINES_FRAGMENT = "inbound/customer-return-form :: linesCard";

    private final CustomerReturnService returns;
    private final CustomerService customers;
    private final ItemResolver itemResolver;
    private final WarehouseContext warehouseContext;

    @GetMapping("/new")
    String newForm(Model model) {
        warehouseContext.require();                       // fail early when the user has no warehouse
        model.addAttribute("form", new CustomerReturnForm());
        prepare(model);
        return FORM_VIEW;
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") CustomerReturnForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        if (form.getLines().stream().allMatch(ReturnLineForm::blank)) {
            result.reject("inbound.return.form.noLines", "Add at least one item");
        }
        Map<Integer, UUID> itemIds = resolveItems(form, result);
        if (!result.hasErrors()) {
            try {
                CustomerReturnResponse created = returns.create(form.toRequest(warehouseId, itemIds));
                redirect.addFlashAttribute("flash", Flash.success("inbound.return.created", created.returnNumber()));
                return "redirect:/customer-returns/" + created.id();
            } catch (BusinessException e) {                // ConflictException is a BusinessException too
                FormErrors.apply(result, e, Map.of());
            }
        }
        prepare(model);
        return FORM_VIEW;
    }

    // ------------------------------------------------------------------ repeating lines (htmx: post the whole form)

    @PostMapping("/lines/add")
    String addLine(@ModelAttribute("form") CustomerReturnForm form, BindingResult result) {
        form.getLines().add(new ReturnLineForm());
        return LINES_FRAGMENT;
    }

    @PostMapping("/lines/remove")
    String removeLine(@RequestParam int index, @ModelAttribute("form") CustomerReturnForm form, BindingResult result) {
        if (form.getLines().size() > 1 && index >= 0 && index < form.getLines().size()) {
            form.getLines().remove(index);
        }
        return LINES_FRAGMENT;
    }

    // ------------------------------------------------------------------ helpers

    private void prepare(Model model) {
        model.addAttribute("menu", "new-return");
        model.addAttribute("customers", customers.findAllActive());
    }

    /** Turns the text typed in each row into an item id. Problems become field errors on that row. */
    private Map<Integer, UUID> resolveItems(CustomerReturnForm form, BindingResult result) {
        Map<Integer, UUID> itemIds = new HashMap<>();
        for (int i = 0; i < form.getLines().size(); i++) {
            ReturnLineForm line = form.getLines().get(i);
            if (line.blank()) {
                continue;
            }
            String itemField = "lines[" + i + "].item";
            if (line.getQuantity() == null) {
                result.rejectValue("lines[" + i + "].quantity", "inbound.line.qty.required", "Enter a quantity");
            }
            if (Codes.blankToNull(line.getItem()) == null) {
                result.rejectValue(itemField, "inbound.item.required", "Enter an item");
                continue;
            }
            ItemResolver.Resolution resolution = itemResolver.resolve(line.getItem());
            if (resolution.found()) {
                itemIds.put(i, resolution.item().id());
            } else if (resolution.ambiguous()) {
                result.rejectValue(itemField, "inbound.item.ambiguous",
                        new Object[]{resolution.candidateSkus()}, "Several items match");
            } else {
                result.rejectValue(itemField, "inbound.item.unknown", "No item matches");
            }
        }
        return itemIds;
    }
}
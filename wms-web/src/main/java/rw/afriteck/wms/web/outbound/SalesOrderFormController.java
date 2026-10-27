package rw.afriteck.wms.web.outbound;

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
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.outbound.dtos.SalesOrderResponse;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.inventory.ItemResolver;
import rw.afriteck.wms.web.support.Flash;
import rw.afriteck.wms.web.support.FormErrors;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
class SalesOrderFormController {

    private static final String FORM_VIEW = "outbound/sales-order-form";
    private static final String LINES_FRAGMENT = "outbound/sales-order-form :: linesCard";

    /** Service error codes that belong to one field; everything else becomes a global error. */
    private static final Map<String, String> ERROR_FIELDS = Map.of(
            "CUSTOMER_INACTIVE", "customerId",
            "SO_DATES_INVALID", "requiredDate",
            "CURRENCY_INVALID", "currency");

    private final SalesOrderService salesOrders;
    private final CustomerService customers;
    private final ItemResolver itemResolver;
    private final UserContext userContext;
    private final WarehouseContext warehouseContext;
    private final Clock clock;

    // ------------------------------------------------------------------ create

    @GetMapping("/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        warehouseContext.require();                       // fail early when the user has no warehouse
        SalesOrderForm form = new SalesOrderForm();
        form.setOrderDate(LocalDate.now(clock));
        model.addAttribute("form", form);
        prepare(model, false, null, null, null);
        return FORM_VIEW;
    }

    @PostMapping
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") SalesOrderForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        if (form.getCustomerId() == null) {
            result.rejectValue("customerId", "so.customer.required", "Choose a customer");
        }
        Map<Integer, UUID> itemIds = resolveItems(form, result);
        if (!result.hasErrors()) {
            try {
                SalesOrderResponse o = salesOrders.create(form.toCreateRequest(warehouseId, itemIds));
                redirect.addFlashAttribute("flash", Flash.success("so.created", o.orderNumber()));
                return "redirect:/sales-orders/" + o.id();
            } catch (BusinessException e) {                // ConflictException is a BusinessException too
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        prepare(model, false, null, null, null);
        return FORM_VIEW;
    }

    // ------------------------------------------------------------------ edit (DRAFT only)

    @GetMapping("/{id}/edit")
    @PreAuthorize(WebAccess.MANAGER)
    String editForm(@PathVariable UUID id, Model model) {
        SalesOrderResponse o = salesOrders.getById(id);
        userContext.requireWarehouseAccess(o.warehouseId());
        if (o.status() != SalesOrderStatus.DRAFT) {
            return "redirect:/sales-orders/" + id;         // the detail hides the Edit button anyway
        }
        model.addAttribute("form", SalesOrderForm.from(o));
        prepare(model, true, id, o.orderNumber(), o.customerName());
        return FORM_VIEW;
    }

    @PostMapping("/{id}/update")
    @PreAuthorize(WebAccess.MANAGER)
    String update(@PathVariable UUID id, @Valid @ModelAttribute("form") SalesOrderForm form,
                  BindingResult result, Model model, RedirectAttributes redirect) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        Map<Integer, UUID> itemIds = resolveItems(form, result);
        if (!result.hasErrors()) {
            try {
                SalesOrderResponse o = salesOrders.update(id, form.toUpdateRequest(itemIds));
                redirect.addFlashAttribute("flash", Flash.success("so.updated", o.orderNumber()));
                return "redirect:/sales-orders/" + id;
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        SalesOrderResponse o = salesOrders.getById(id);            // header of the page
        prepare(model, true, id, o.orderNumber(), o.customerName());
        return FORM_VIEW;
    }

    // ------------------------------------------------------------------ repeating lines (htmx: post the whole form)

    @PostMapping("/lines/add")
    @PreAuthorize(WebAccess.MANAGER)
    String addLine(@ModelAttribute("form") SalesOrderForm form, BindingResult result) {
        form.getLines().add(new SalesOrderLineForm());
        return LINES_FRAGMENT;
    }

    @PostMapping("/lines/remove")
    @PreAuthorize(WebAccess.MANAGER)
    String removeLine(@RequestParam int index, @ModelAttribute("form") SalesOrderForm form, BindingResult result) {
        if (form.getLines().size() > 1 && index >= 0 && index < form.getLines().size()) {
            form.getLines().remove(index);
        }
        return LINES_FRAGMENT;
    }

    // ------------------------------------------------------------------ helpers

    private void prepare(Model model, boolean editing, UUID soId, String orderNumber, String customerName) {
        model.addAttribute("menu", editing ? "sales-orders" : "new-so");
        model.addAttribute("editing", editing);
        if (editing) {
            model.addAttribute("soId", soId);
            model.addAttribute("soNumber", orderNumber);
            model.addAttribute("customerName", customerName);
        } else {
            model.addAttribute("customers", customers.findAllActive());
        }
    }

    /** Turns the text typed in each row into an item id. Problems become field errors on that row. */
    private Map<Integer, UUID> resolveItems(SalesOrderForm form, BindingResult result) {
        Map<Integer, UUID> itemIds = new HashMap<>();
        for (int i = 0; i < form.getLines().size(); i++) {
            SalesOrderLineForm line = form.getLines().get(i);
            if (line.blank()) {
                continue;
            }
            String itemField = "lines[" + i + "].item";
            if (line.getQuantity() == null) {
                result.rejectValue("lines[" + i + "].quantity", "so.line.qty.required", "Enter a quantity");
            }
            if (Codes.blankToNull(line.getItem()) == null) {
                result.rejectValue(itemField, "so.item.required", "Enter an item");
                continue;
            }
            ItemResolver.Resolution resolution = itemResolver.resolve(line.getItem());
            if (resolution.found()) {
                itemIds.put(i, resolution.item().id());
            } else if (resolution.ambiguous()) {
                result.rejectValue(itemField, "so.item.ambiguous",
                        new Object[]{resolution.candidateSkus()}, "Several items match");
            } else {
                result.rejectValue(itemField, "so.item.unknown", "No item matches");
            }
        }
        return itemIds;
    }
}

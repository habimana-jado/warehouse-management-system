package rw.afriteck.wms.web.inbound;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.service.SupplierService;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.inventory.ItemResolver;
import rw.afriteck.wms.web.support.Flash;
import rw.afriteck.wms.web.support.FormErrors;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
class PurchaseOrderFormController {

    private static final String FORM_VIEW = "inbound/purchase-order-form";
    private static final String LINES_FRAGMENT = "inbound/purchase-order-form :: linesCard";

    /** Service error codes that belong to one field; everything else becomes a global error. */
    private static final Map<String, String> ERROR_FIELDS = Map.of(
            "SUPPLIER_INACTIVE", "supplierId",
            "PO_DATES_INVALID", "expectedDate",
            "CURRENCY_INVALID", "currency");

    private final PurchaseOrderService purchaseOrders;
    private final SupplierService suppliers;
    private final ItemResolver itemResolver;
    private final UserContext userContext;
    private final WarehouseContext warehouseContext;

    // ------------------------------------------------------------------ create

    @GetMapping("/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        warehouseContext.require();                       // fail early when the user has no warehouse
        model.addAttribute("form", new PurchaseOrderForm());
        prepare(model, false, null, null, null);
        return FORM_VIEW;
    }

    @PostMapping
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") PurchaseOrderForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        if (form.getSupplierId() == null) {
            result.rejectValue("supplierId", "inbound.po.supplier.required", "Choose a supplier");
        }
        Map<Integer, UUID> itemIds = resolveItems(form, result);
        if (!result.hasErrors()) {
            try {
                PurchaseOrderResponse po = purchaseOrders.create(form.toCreateRequest(warehouseId, itemIds));
                redirect.addFlashAttribute("flash", Flash.success("inbound.po.created", po.poNumber()));
                return "redirect:/purchase-orders/" + po.id();
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
        PurchaseOrderResponse po = purchaseOrders.getById(id);
        userContext.requireWarehouseAccess(po.warehouseId());
        if (po.status() != PurchaseOrderStatus.DRAFT) {
            return "redirect:/purchase-orders/" + id;      // the detail hides the Edit button anyway
        }
        model.addAttribute("form", PurchaseOrderForm.from(po));
        prepare(model, true, id, po.poNumber(), po.supplierName());
        return FORM_VIEW;
    }

    @PostMapping("/{id}/update")
    @PreAuthorize(WebAccess.MANAGER)
    String update(@PathVariable UUID id, @Valid @ModelAttribute("form") PurchaseOrderForm form,
                  BindingResult result, Model model, RedirectAttributes redirect) {
        userContext.requireWarehouseAccess(purchaseOrders.warehouseIdOf(id));
        Map<Integer, UUID> itemIds = resolveItems(form, result);
        if (!result.hasErrors()) {
            try {
                PurchaseOrderResponse po = purchaseOrders.update(id, form.toUpdateRequest(itemIds));
                redirect.addFlashAttribute("flash", Flash.success("inbound.po.updated", po.poNumber()));
                return "redirect:/purchase-orders/" + id;
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        PurchaseOrderResponse po = purchaseOrders.getById(id);     // header of the page
        prepare(model, true, id, po.poNumber(), po.supplierName());
        return FORM_VIEW;
    }

    // ------------------------------------------------------------------ repeating lines (htmx: post the whole form)

    @PostMapping("/lines/add")
    @PreAuthorize(WebAccess.MANAGER)
    String addLine(@ModelAttribute("form") PurchaseOrderForm form, BindingResult result) {
        form.getLines().add(new PurchaseOrderLineForm());
        return LINES_FRAGMENT;
    }

    @PostMapping("/lines/remove")
    @PreAuthorize(WebAccess.MANAGER)
    String removeLine(@RequestParam int index, @ModelAttribute("form") PurchaseOrderForm form, BindingResult result) {
        if (form.getLines().size() > 1 && index >= 0 && index < form.getLines().size()) {
            form.getLines().remove(index);
        }
        return LINES_FRAGMENT;
    }

    // ------------------------------------------------------------------ helpers

    private void prepare(Model model, boolean editing, UUID poId, String poNumber, String supplierName) {
        model.addAttribute("menu", editing ? "purchase-orders" : "new-po");
        model.addAttribute("editing", editing);
        if (editing) {
            model.addAttribute("poId", poId);
            model.addAttribute("poNumber", poNumber);
            model.addAttribute("supplierName", supplierName);
        } else {
            model.addAttribute("suppliers", suppliers.findAllActive());
        }
    }

    /** Turns the text typed in each row into an item id. Problems become field errors on that row. */
    private Map<Integer, UUID> resolveItems(PurchaseOrderForm form, BindingResult result) {
        Map<Integer, UUID> itemIds = new HashMap<>();
        for (int i = 0; i < form.getLines().size(); i++) {
            PurchaseOrderLineForm line = form.getLines().get(i);
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
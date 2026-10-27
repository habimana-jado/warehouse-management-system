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
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderLineResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.enums.ReceiptMode;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.inventory.BinOptions;
import rw.afriteck.wms.web.support.Flash;
import rw.afriteck.wms.web.support.FormErrors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/purchase-orders/{id}/receive")
@PreAuthorize(WebAccess.INBOUND_RECEIVE)
@RequiredArgsConstructor
class ReceivingPageController {

    private static final String VIEW = "inbound/receive-form";
    private static final String LINES_FRAGMENT = "inbound/receive-form :: linesCard";
    private static final Map<String, String> ERROR_FIELDS = Map.of(
            "NO_RECEIVING_LOCATION", "receivingLocationId",
            "NO_DAMAGED_LOCATION", "damagedLocationId");

    private final PurchaseOrderService purchaseOrders;
    private final GoodsReceiptService goodsReceipts;
    private final ItemService itemService;
    private final BinOptions binOptions;
    private final UserContext userContext;

    @GetMapping
    String form(@PathVariable UUID id, Model model) {
        PurchaseOrderResponse po = loadPo(id);
        if (!receivable(po)) {
            return "redirect:/purchase-orders/" + id;
        }
        model.addAttribute("form", ReceiveForm.forPurchaseOrder(po));
        prepare(model, po);
        return VIEW;
    }

    @PostMapping
    String receive(@PathVariable UUID id, @Valid @ModelAttribute("form") ReceiveForm form, BindingResult result,
                   Model model, RedirectAttributes redirect) {
        PurchaseOrderResponse po = loadPo(id);
        if (!receivable(po)) {
            return "redirect:/purchase-orders/" + id;
        }
        if (form.getLines().stream().allMatch(ReceiveLineForm::blank)) {
            result.reject("inbound.receive.nothing", "Enter the quantities that arrived");
        }
        if (!result.hasErrors()) {
            try {
                GoodsReceiptResponse receipt = goodsReceipts.receive(id, form.toRequest(), userContext.currentUserId());
                redirect.addFlashAttribute("flash", Flash.success("inbound.receive.posted", receipt.receiptNumber()));
                return "redirect:/goods-receipts/" + receipt.id();
            } catch (BusinessException e) {               // includes ConflictException (for example a PO closed meanwhile)
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        prepare(model, po);
        return VIEW;
    }

    // ------------------------------------------------------------------ rows (htmx: post the whole form)

    @PostMapping("/lines/add")
    String addRow(@PathVariable UUID id, @RequestParam UUID poLineId,
                  @ModelAttribute("form") ReceiveForm form, BindingResult result, Model model) {
        PurchaseOrderResponse po = loadPo(id);
        ReceiveLineForm row = new ReceiveLineForm();
        row.setPurchaseOrderLineId(poLineId);
        int at = form.getLines().size();                    // keep the rows of one PO line together
        for (int i = form.getLines().size() - 1; i >= 0; i--) {
            if (poLineId.equals(form.getLines().get(i).getPurchaseOrderLineId())) {
                at = i + 1;
                break;
            }
        }
        form.getLines().add(at, row);
        prepare(model, po);
        return LINES_FRAGMENT;
    }

    @PostMapping("/lines/remove")
    String removeRow(@PathVariable UUID id, @RequestParam int index,
                     @ModelAttribute("form") ReceiveForm form, BindingResult result, Model model) {
        PurchaseOrderResponse po = loadPo(id);
        if (index >= 0 && index < form.getLines().size()) {
            UUID poLineId = form.getLines().get(index).getPurchaseOrderLineId();
            long sameLine = form.getLines().stream().filter(l -> java.util.Objects.equals(poLineId, l.getPurchaseOrderLineId())).count();
            if (sameLine > 1) {                              // every PO line keeps at least one row
                form.getLines().remove(index);
            }
        }
        prepare(model, po);
        return LINES_FRAGMENT;
    }

    // ------------------------------------------------------------------ helpers

    private PurchaseOrderResponse loadPo(UUID id) {
        PurchaseOrderResponse po = purchaseOrders.getById(id);             // NotFoundException -> error/404
        userContext.requireWarehouseAccess(po.warehouseId());
        return po;
    }

    private static boolean receivable(PurchaseOrderResponse po) {
        return PurchaseOrderStatus.RECEIVABLE.contains(po.status());
    }

    /** Everything the page and the lines fragment need besides the form itself. */
    private void prepare(Model model, PurchaseOrderResponse po) {
        Set<UUID> itemIds = po.lines().stream().map(PurchaseOrderLineResponse::itemId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> items = itemService.findAllByIds(itemIds).stream()
                .collect(Collectors.toMap(ItemResponse::id, Function.identity()));
        Map<UUID, ReceiveLineInfo> info = new HashMap<>();
        for (PurchaseOrderLineResponse l : po.lines()) {
            info.put(l.id(), ReceiveLineInfo.of(l, items.get(l.itemId())));
        }

        List<BinOptions.BinOption> bins = binOptions.forWarehouse(po.warehouseId());
        model.addAttribute("po", po);
        model.addAttribute("poId", po.id());
        model.addAttribute("lineInfo", info);
        model.addAttribute("modes", List.of(ReceiptMode.values()));
        model.addAttribute("receivingBins", bins.stream().filter(b -> "RECEIVING".equals(b.usage().name())).toList());
        model.addAttribute("storageBins", bins.stream().filter(b -> "STORAGE".equals(b.usage().name())).toList());
        model.addAttribute("damagedBins", bins.stream().filter(b -> "DAMAGED".equals(b.usage().name())).toList());

        Map<UUID, Long> rowCounts = new HashMap<>();
        Object form = model.getAttribute("form");
        if (form instanceof ReceiveForm f) {
            f.getLines().forEach(l -> rowCounts.merge(l.getPurchaseOrderLineId(), 1L, Long::sum));
        }
        model.addAttribute("rowCounts", rowCounts);
    }
}
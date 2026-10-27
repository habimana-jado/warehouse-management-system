package rw.afriteck.wms.web.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.CreatePurchaseOrderRequest;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderLineRequest;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderLineResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.dtos.UpdatePurchaseOrderRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Create and edit form. supplierId and orderDate are only used when creating. */
@Getter
@Setter
public class PurchaseOrderForm {

    private UUID supplierId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedDate;

    @Size(max = 100)
    private String supplierReference;

    @Size(max = 3)
    private String currency;

    @Size(max = 1000)
    private String notes;

    @Valid
    private List<PurchaseOrderLineForm> lines = new ArrayList<>(List.of(new PurchaseOrderLineForm()));

    /** Fills the form from an existing PO (edit screen). Quantities and prices are in the item base unit. */
    static PurchaseOrderForm from(PurchaseOrderResponse po) {
        PurchaseOrderForm form = new PurchaseOrderForm();
        form.expectedDate = po.expectedDate();
        form.supplierReference = po.supplierReference();
        form.currency = po.currency();
        form.notes = po.notes();
        form.lines = new ArrayList<>();
        for (PurchaseOrderLineResponse l : po.lines()) {
            PurchaseOrderLineForm line = new PurchaseOrderLineForm();
            line.setLineId(l.id());
            line.setItem(l.itemSku());
            line.setQuantity(l.orderedQty());
            line.setUnitPrice(l.unitPrice());
            line.setTolerancePercent(l.tolerancePercent());
            line.setNotes(l.notes());
            form.lines.add(line);
        }
        if (form.lines.isEmpty()) {
            form.lines.add(new PurchaseOrderLineForm());
        }
        return form;
    }

    CreatePurchaseOrderRequest toCreateRequest(UUID warehouseId, Map<Integer, UUID> itemIds) {
        return new CreatePurchaseOrderRequest(supplierId, warehouseId, orderDate, expectedDate,
                Codes.blankToNull(supplierReference), Codes.blankToNull(currency), Codes.blankToNull(notes),
                lineRequests(itemIds));
    }

    UpdatePurchaseOrderRequest toUpdateRequest(Map<Integer, UUID> itemIds) {
        return new UpdatePurchaseOrderRequest(expectedDate,
                Codes.blankToNull(supplierReference), Codes.blankToNull(currency), Codes.blankToNull(notes),
                lineRequests(itemIds));
    }

    /** itemIds: row index -> resolved item id. Blank rows are skipped. */
    private List<PurchaseOrderLineRequest> lineRequests(Map<Integer, UUID> itemIds) {
        List<PurchaseOrderLineRequest> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            PurchaseOrderLineForm l = lines.get(i);
            if (l.blank()) {
                continue;
            }
            out.add(new PurchaseOrderLineRequest(l.getLineId(), itemIds.get(i), l.getQuantity(),
                    Codes.blankToNull(l.getUom()), l.getUnitPrice(), l.getTolerancePercent(),
                    Codes.blankToNull(l.getNotes())));
        }
        return out;
    }
}
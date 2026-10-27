package rw.afriteck.wms.web.outbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.support.OutboundDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
class SalesOrderForm {

    private UUID customerId;                                       // create only: the customer cannot change afterwards
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate requiredDate;
    @Size(max = 3)
    private String currency = OutboundDefaults.DEFAULT_CURRENCY;
    @Size(max = 60)
    private String customerReference;
    @Size(max = 1000)
    private String notes;
    @Valid
    private List<SalesOrderLineForm> lines = new ArrayList<>(List.of(new SalesOrderLineForm()));

    static SalesOrderForm from(SalesOrderResponse o) {
        SalesOrderForm f = new SalesOrderForm();
        f.setOrderDate(o.orderDate());
        f.setRequiredDate(o.requiredDate());
        f.setCurrency(o.currency());
        f.setCustomerReference(o.customerReference());
        f.setNotes(o.notes());
        f.setLines(new ArrayList<>());
        for (SalesOrderLineResponse l : o.lines()) {
            SalesOrderLineForm lf = new SalesOrderLineForm();
            lf.setItem(l.itemSku());                               // the SKU resolves exactly again on save
            lf.setQuantity(l.orderedQty());                        // stored in the base unit: the unit field stays empty
            lf.setUnitPrice(l.unitPrice());
            lf.setNote(l.note());
            f.getLines().add(lf);
        }
        if (f.getLines().isEmpty()) {
            f.getLines().add(new SalesOrderLineForm());
        }
        return f;
    }

    CreateSalesOrderRequest toCreateRequest(UUID warehouseId, Map<Integer, UUID> itemIds) {
        return new CreateSalesOrderRequest(customerId, warehouseId, orderDate, requiredDate,
                Codes.blankToNull(currency), Codes.blankToNull(customerReference), Codes.blankToNull(notes),
                requestLines(itemIds));
    }

    UpdateSalesOrderRequest toUpdateRequest(Map<Integer, UUID> itemIds) {
        return new UpdateSalesOrderRequest(orderDate, requiredDate, Codes.blankToNull(currency),
                Codes.blankToNull(customerReference), Codes.blankToNull(notes), requestLines(itemIds));
    }

    /** Blank rows are ignored; every other row has an item id resolved by the controller. */
    private List<SalesOrderLineRequest> requestLines(Map<Integer, UUID> itemIds) {
        List<SalesOrderLineRequest> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            SalesOrderLineForm l = lines.get(i);
            if (l.blank()) continue;
            out.add(new SalesOrderLineRequest(itemIds.get(i), l.getQuantity(), Codes.blankToNull(l.getUom()),
                    l.getUnitPrice(), Codes.blankToNull(l.getNote())));
        }
        return out;
    }
}


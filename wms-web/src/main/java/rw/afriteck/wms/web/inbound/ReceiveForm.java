package rw.afriteck.wms.web.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderLineResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.dtos.ReceiveGoodsLineRequest;
import rw.afriteck.wms.inbound.dtos.ReceiveGoodsRequest;
import rw.afriteck.wms.inbound.enums.ReceiptMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReceiveForm {

    private ReceiptMode mode = ReceiptMode.PUTAWAY;

    private UUID receivingLocationId;         // empty = first active RECEIVING bin
    private UUID damagedLocationId;           // empty = first active DAMAGED bin

    @Size(max = 100)
    private String deliveryNoteNumber;

    @Size(max = 1000)
    private String notes;

    @Valid
    private List<ReceiveLineForm> lines = new ArrayList<>();

    /** One empty row per PO line that can still accept goods (tolerance included). */
    static ReceiveForm forPurchaseOrder(PurchaseOrderResponse po) {
        ReceiveForm form = new ReceiveForm();
        for (PurchaseOrderLineResponse l : po.lines()) {
            if (l.acceptableQtyLeft().signum() > 0) {
                ReceiveLineForm row = new ReceiveLineForm();
                row.setPurchaseOrderLineId(l.id());
                form.lines.add(row);
            }
        }
        return form;
    }

    ReceiveGoodsRequest toRequest() {
        List<ReceiveGoodsLineRequest> out = new ArrayList<>();
        for (ReceiveLineForm l : lines) {
            if (l.blank()) {
                continue;
            }
            out.add(new ReceiveGoodsLineRequest(l.getPurchaseOrderLineId(), Codes.blankToNull(l.getUom()),
                    l.getAcceptedQty(), l.getRejectedQty(), Codes.blankToNull(l.getRejectReason()),
                    Codes.blankToNull(l.getLotNumber()), l.getExpiryDate(), Codes.blankToNull(l.getSerialNumber()),
                    l.getLocationId()));
        }
        return new ReceiveGoodsRequest(mode, receivingLocationId, damagedLocationId,
                Codes.blankToNull(deliveryNoteNumber), Codes.blankToNull(notes), out);
    }
}
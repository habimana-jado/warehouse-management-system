package rw.afriteck.wms.web.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.ReceiveLineRequest;
import rw.afriteck.wms.inventory.dtos.ReceiveTransferRequest;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReceiveForm {

    @Valid
    private List<ReceiveLineForm> lines = new ArrayList<>();
    private boolean closeShort;
    @Size(max = 500) private String shortReason;

    /** One row per line still in transit, pre-filled with the whole remaining quantity. */
    static ReceiveForm from(List<TransferLineRow> receivable) {
        ReceiveForm f = new ReceiveForm();
        for (TransferLineRow r : receivable) {
            f.lines.add(new ReceiveLineForm(r.line().id(), r.line().toLocationId(), r.line().inTransitQty()));
        }
        return f;
    }

    ReceiveTransferRequest toRequest() {
        List<ReceiveLineRequest> out = lines.stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().signum() > 0)
                .map(l -> new ReceiveLineRequest(l.getLineId(), l.getToLocationId(), l.getQuantity()))
                .toList();
        return new ReceiveTransferRequest(out, closeShort, Codes.blankToNull(shortReason));
    }
}

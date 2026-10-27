package rw.afriteck.wms.web.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.CustomerReturnLineResponse;
import rw.afriteck.wms.inbound.dtos.CustomerReturnResponse;
import rw.afriteck.wms.inbound.dtos.ReceiveCustomerReturnRequest;
import rw.afriteck.wms.inbound.dtos.ReceiveReturnLineRequest;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReceiveReturnForm {

    @Size(max = 1000)
    private String notes;

    @Valid
    private List<ReceiveReturnLineForm> lines = new ArrayList<>();

    /** One row per return line, in the same order as the lines on screen; lot, expiry and serial prefilled from the expected values. */
    static ReceiveReturnForm from(CustomerReturnResponse ret) {
        ReceiveReturnForm form = new ReceiveReturnForm();
        for (CustomerReturnLineResponse l : ret.lines()) {
            ReceiveReturnLineForm row = new ReceiveReturnLineForm();
            row.setLineId(l.id());
            row.setLotNumber(l.lotNumber());
            row.setExpiryDate(l.expiryDate());
            row.setSerialNumber(l.serialNumber());
            form.lines.add(row);
        }
        return form;
    }

    ReceiveCustomerReturnRequest toRequest() {
        List<ReceiveReturnLineRequest> out = new ArrayList<>();
        for (ReceiveReturnLineForm l : lines) {
            if (l.blank()) {
                continue;
            }
            out.add(new ReceiveReturnLineRequest(l.getLineId(), null, l.getReceivedQty(), l.getDisposition(),
                    l.getLocationId(), Codes.blankToNull(l.getLotNumber()), l.getExpiryDate(),
                    Codes.blankToNull(l.getSerialNumber())));
        }
        return new ReceiveCustomerReturnRequest(Codes.blankToNull(notes), out);
    }
}
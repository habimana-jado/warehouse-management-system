package rw.afriteck.wms.web.outbound;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.ConfirmPickRequest;
import rw.afriteck.wms.outbound.dtos.PickAllocationRequest;
import rw.afriteck.wms.outbound.dtos.PickLineResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
class PickConfirmForm {
    private List<PickAllocationForm> allocations = new ArrayList<>();
    @Size(max = 500)
    private String shortReason;                                    // needed when less than planned is picked
    private boolean expiryOverride;                                // honoured only for managers

    /**
     * One row prefilled with what the suggester proposed, for the full planned quantity.
     */
    static PickConfirmForm suggested(PickLineResponse l) {
        PickConfirmForm f = new PickConfirmForm();
        PickAllocationForm a = new PickAllocationForm();
        a.setLocationId(l.suggestedLocationId());
        a.setLotNumber(l.suggestedLotNumber());
        if (!"SERIAL".equals(trackingName(l))) {
            a.setQuantity(l.plannedQty());
        }
        f.getAllocations().add(a);
        return f;
    }

    /**
     * Serial items become one allocation per serial (quantity 1, same bin); everything else is one allocation per row.
     */
    ConfirmPickRequest toRequest(boolean serial) {
        List<PickAllocationRequest> out = new ArrayList<>();
        for (PickAllocationForm a : allocations) {
            if (a.blank(serial)) continue;
            if (serial) {
                for (String s : a.serialList()) {
                    out.add(new PickAllocationRequest(a.getLocationId(), null, s, BigDecimal.ONE));
                }
            } else {
                out.add(new PickAllocationRequest(a.getLocationId(), Codes.blankToNull(a.getLotNumber()), null, a.getQuantity()));
            }
        }
        return new ConfirmPickRequest(out, Codes.blankToNull(shortReason), expiryOverride);
    }

    static String trackingName(PickLineResponse l) {
        return l.trackingType() == null ? "NONE" : l.trackingType().name();
    }
}

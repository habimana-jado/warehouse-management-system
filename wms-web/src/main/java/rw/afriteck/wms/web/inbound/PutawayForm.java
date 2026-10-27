package rw.afriteck.wms.web.inbound;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.ConfirmPutawayRequest;

import java.math.BigDecimal;
import java.util.UUID;

/** Inline put-away confirmation of one receipt line. Everything is optional: empty means the suggested bin and the full quantity. */
@Getter
@Setter
public class PutawayForm {

    private UUID locationId;
    private BigDecimal doneQty;
    private String shortReason;

    ConfirmPutawayRequest toRequest() {
        return new ConfirmPutawayRequest(locationId, doneQty, Codes.blankToNull(shortReason));
    }
}
package rw.afriteck.wms.web.outbound;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
class ShipLineForm {
    private UUID salesOrderLineId;
    private BigDecimal quantity;                                   // empty or 0 = not shipped now
}


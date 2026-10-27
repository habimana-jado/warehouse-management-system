package rw.afriteck.wms.web.outbound;

import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
class PickAllocationForm {
    private UUID locationId;
    private String lotNumber;                                      // LOT items
    private BigDecimal quantity;                                   // not used for SERIAL items
    private String serials;                                        // SERIAL items: one serial per line, comma or space

    /** A row without anything meaningful is ignored. */
    boolean blank(boolean serial) {
        return serial ? Codes.blankToNull(serials) == null
                : locationId == null && quantity == null && Codes.blankToNull(lotNumber) == null;
    }

    List<String> serialList() {
        if (serials == null) return List.of();
        return Arrays.stream(serials.split("[\\s,;]+")).filter(s -> !s.isBlank()).toList();
    }
}
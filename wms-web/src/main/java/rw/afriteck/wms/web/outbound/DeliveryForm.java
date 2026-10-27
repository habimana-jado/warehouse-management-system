package rw.afriteck.wms.web.outbound;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.ConfirmDeliveryRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
class DeliveryForm {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deliveredAt;                             // empty = now
    @Size(max = 120)
    private String receivedByName;
    @Size(max = 500)
    private String podNotes;

    ConfirmDeliveryRequest toRequest(ZoneId zone) {
        Instant at = deliveredAt == null ? null : deliveredAt.atZone(zone).toInstant();
        return new ConfirmDeliveryRequest(at, Codes.blankToNull(receivedByName), Codes.blankToNull(podNotes));
    }
}

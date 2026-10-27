package rw.afriteck.wms.web.outbound;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class CancelPickListForm {
    @Size(max = 500)
    private String reason;
}

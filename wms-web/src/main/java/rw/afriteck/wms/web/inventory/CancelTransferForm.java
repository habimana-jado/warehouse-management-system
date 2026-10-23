package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelTransferForm {
    @NotBlank
    @Size(max = 500) private String reason;
}

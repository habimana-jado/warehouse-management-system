package rw.afriteck.wms.web.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;

@Getter
@Setter
public class AdjustmentReasonForm {

    @NotBlank
    @Size(max = 32) private String code;          // on edit it is shown read-only (and ignored: the code is immutable)
    @NotBlank @Size(max = 120) private String name;
    @NotNull
    private AdjustmentDirection direction = AdjustmentDirection.BOTH;
    private boolean requiresApproval = true;                // least privilege default: approval required
    private boolean active = true;                          // edit only

    static AdjustmentReasonForm from(AdjustmentReasonResponse r) {
        AdjustmentReasonForm f = new AdjustmentReasonForm();
        f.setCode(r.code());
        f.setName(r.name());
        f.setDirection(r.direction());
        f.setRequiresApproval(r.requiresApproval());
        f.setActive(r.active());
        return f;
    }

    CreateAdjustmentReasonRequest toCreateRequest() {
        return new CreateAdjustmentReasonRequest(code, Codes.blankToNull(name), direction, requiresApproval);
    }

    UpdateAdjustmentReasonRequest toUpdateRequest() {
        return new UpdateAdjustmentReasonRequest(Codes.blankToNull(name), direction, requiresApproval, active);
    }
}

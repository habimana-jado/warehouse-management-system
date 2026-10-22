package rw.afriteck.wms.web.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.operations.dtos.CancelTaskRequest;

@Getter @Setter
public class CancelTaskForm {
    @NotBlank
    @Size(max = 255) private String reason;
    public CancelTaskRequest toRequest() { return new CancelTaskRequest(reason.trim()); }
}

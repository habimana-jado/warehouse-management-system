package rw.afriteck.wms.web.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.operations.dtos.ChangePriorityRequest;
import rw.afriteck.wms.operations.enums.TaskPriority;

@Getter @Setter
public class ChangePriorityForm {
    @NotNull private TaskPriority priority;
    public ChangePriorityRequest toRequest() { return new ChangePriorityRequest(priority); }
}

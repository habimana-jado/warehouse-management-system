package rw.afriteck.wms.web.operations;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.operations.dtos.AssignTaskRequest;

import java.util.UUID;

@Getter @Setter
public class AssignTaskForm {
    @NotNull
    private UUID assigneeId;
    public AssignTaskRequest toRequest() { return new AssignTaskRequest(assigneeId); }
}

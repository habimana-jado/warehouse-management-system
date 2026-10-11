package rw.afriteck.wms.operations.guard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.repository.TaskRepository;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseTasksDeactivationGuard implements DeactivationGuard {

    private final TaskRepository taskRepository;

    @Override
    public Target target() {
        return Target.WAREHOUSE;
    }

    @Override
    public void assertCanDeactivate(UUID id) {
        if (taskRepository.existsByWarehouseIdAndStatusIn(id, TaskStatus.ACTIVE)) {
            throw new ConflictException("WAREHOUSE_HAS_OPEN_TASKS",
                    "Warehouse still has open, assigned or in-progress tasks");
        }
    }
}

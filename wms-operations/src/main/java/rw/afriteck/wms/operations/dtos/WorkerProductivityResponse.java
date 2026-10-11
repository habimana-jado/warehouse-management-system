package rw.afriteck.wms.operations.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkerProductivityResponse(
        UUID userId, String userName,
        long tasksCompleted, long linesCompleted,
        BigDecimal totalQtyDone,
        long shortLines,
        Long avgTaskSeconds                          // completedAt - startedAt; null if no timed task
) {}

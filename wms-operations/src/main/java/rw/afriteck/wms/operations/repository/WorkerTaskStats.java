package rw.afriteck.wms.operations.repository;

import java.util.UUID;

public interface WorkerTaskStats {
    UUID getUserId();
    long getTasksCompleted();
    Double getAvgTaskSeconds();      // null when no task has startedAt
}

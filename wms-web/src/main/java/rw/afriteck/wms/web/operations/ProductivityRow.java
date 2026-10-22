package rw.afriteck.wms.web.operations;

import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductivityRow(UUID userId, String userName, long tasksCompleted, long linesCompleted,
                              BigDecimal totalQtyDone, long shortLines, Long avgMinutes) {
    public static ProductivityRow of(WorkerProductivityResponse r) {
        Long minutes = r.avgTaskSeconds() == null ? null : Math.round(r.avgTaskSeconds() / 60.0);
        return new ProductivityRow(r.userId(), r.userName(), r.tasksCompleted(), r.linesCompleted(),
                r.totalQtyDone(), r.shortLines(), minutes);
    }
}

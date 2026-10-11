package rw.afriteck.wms.operations.repository;

import java.util.UUID;

public interface TaskLineCount {
    UUID getTaskId();
    long getTotal();
    long getDone();     // lines no longer PENDING (DONE, SHORT, SKIPPED)
}

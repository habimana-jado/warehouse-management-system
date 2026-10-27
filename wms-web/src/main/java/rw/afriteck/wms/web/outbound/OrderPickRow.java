package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.PickListResponse;
import rw.afriteck.wms.outbound.enums.PickLineStatus;
import rw.afriteck.wms.outbound.enums.PickListStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Small read model of a pick list for the order detail tab.
 */
record OrderPickRow(UUID id, String pickNumber, PickListStatus status, Instant releasedAt, int lineCount,
                    int doneCount) {
    static OrderPickRow of(PickListResponse p) {
        int done = (int) p.lines().stream().filter(l -> l.status() != PickLineStatus.PENDING).count();
        return new OrderPickRow(p.id(), p.pickNumber(), p.status(), p.releasedAt(), p.lines().size(), done);
    }
}

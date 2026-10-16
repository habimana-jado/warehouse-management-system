package rw.afriteck.wms.outbound.repository;

import java.util.UUID;

public interface ParentCount {
    UUID getParentId();
    long getTotal();
}

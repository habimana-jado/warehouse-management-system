package rw.afriteck.wms.common.config;

import java.util.Set;
import java.util.UUID;

public interface WarehouseIdValidator {
    void validate(Set<UUID> warehouseIds);   // throws BusinessException if any id is unknown
}

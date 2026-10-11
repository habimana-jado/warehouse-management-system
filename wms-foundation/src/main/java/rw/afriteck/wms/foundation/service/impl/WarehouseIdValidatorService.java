package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.config.WarehouseIdValidator;
import rw.afriteck.wms.common.exception.BusinessException;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WarehouseIdValidatorImpl implements WarehouseIdValidator {

//    TODO: Uncomment after writing this repo
//    private final WarehouseRepository warehouses;

    @Override
    @Transactional(readOnly = true)
    public void validate(Set<UUID> ids) {
//        if (ids.isEmpty()) return;
//        long found = warehouses.countByIdIn(ids);
//        if (found != ids.size()) {
//            throw new BusinessException("UNKNOWN_WAREHOUSE", "One or more warehouses do not exist");
//        }
    }
}

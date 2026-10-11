package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.config.WarehouseIdValidator;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.repository.WarehouseRepository;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class WarehouseIdValidatorImpl implements WarehouseIdValidator {

    private final WarehouseRepository warehouses;

    @Override
    @Transactional(readOnly = true)
    public void validate(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        if (warehouses.countByIdIn(ids) != ids.size()) {
            throw new BusinessException("UNKNOWN_WAREHOUSE", "One or more warehouses do not exist");
        }
    }
}

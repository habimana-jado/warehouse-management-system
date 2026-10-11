package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.CreateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.UpdateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.entity.Warehouse;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.mapper.WarehouseMapper;
import rw.afriteck.wms.foundation.repository.WarehouseRepository;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.foundation.support.Codes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouses;
    private final DeactivationGuards guards;
    private final UserContext userContext;

    @Override
    public WarehouseResponse create(CreateWarehouseRequest r) {
        String code = Codes.normalize(r.code());
        if (warehouses.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("WAREHOUSE_CODE_EXISTS", "Warehouse code already exists: " + code);
        }
        Warehouse w = new Warehouse();
        w.setCode(code);
        apply(w, r.name(), r.addressLine(), r.city(), r.country());
        return WarehouseMapper.toResponse(warehouses.save(w));
    }

    @Override
    public WarehouseResponse update(UUID id, UpdateWarehouseRequest r) {
        Warehouse w = find(id);
        apply(w, r.name(), r.addressLine(), r.city(), r.country());
        return WarehouseMapper.toResponse(w);
    }

    @Override
    public WarehouseResponse activate(UUID id) {
        Warehouse w = find(id);
        w.setActive(true);
        return WarehouseMapper.toResponse(w);
    }

    @Override
    public WarehouseResponse deactivate(UUID id) {
        Warehouse w = find(id);
        guards.check(DeactivationGuard.Target.WAREHOUSE, id);
        w.setActive(false);
        return WarehouseMapper.toResponse(w);
    }

    @Override @Transactional(readOnly = true)
    public Page<WarehouseResponse> search(String q, Pageable pageable) {
        String term = q == null ? "" : q.trim();
        return warehouses.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(term, term, pageable)
                .map(WarehouseMapper::toResponse);
    }

    @Override @Transactional(readOnly = true)
    public List<WarehouseResponse> findAccessibleToCurrentUser() {
        if (userContext.isAdmin()) return findAllActive();
        Set<UUID> ids = userContext.accessibleWarehouseIds();
        if (ids.isEmpty()) return List.of();
        return warehouses.findByIdInAndActiveTrueOrderByName(ids).stream()
                .map(WarehouseMapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public WarehouseResponse getById(UUID id) {
        return WarehouseMapper.toResponse(find(id));
    }

    @Override @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return warehouses.existsById(id);
    }

    @Override @Transactional(readOnly = true)
    public List<WarehouseResponse> findAllActive() {
        return warehouses.findByActiveTrueOrderByName().stream()
                .map(WarehouseMapper::toResponse).toList();
    }

    private Warehouse find(UUID id) {
        return warehouses.findById(id).orElseThrow(() -> new NotFoundException("Warehouse", id));
    }

    private void apply(Warehouse w, String name, String address, String city, String country) {
        w.setName(name.trim());
        w.setAddressLine(Codes.blankToNull(address));
        w.setCity(Codes.blankToNull(city));
        w.setCountry(Codes.blankToNull(country));
    }
}

package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.CreateLocationRequest;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;
import rw.afriteck.wms.foundation.dtos.UpdateLocationRequest;
import rw.afriteck.wms.foundation.entity.Location;
import rw.afriteck.wms.foundation.entity.Warehouse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.mapper.LocationMapper;
import rw.afriteck.wms.foundation.repository.LocationRepository;
import rw.afriteck.wms.foundation.repository.WarehouseRepository;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.support.Codes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locations;
    private final WarehouseRepository warehouses;
    private final DeactivationGuards guards;

    @Override
    public LocationResponse create(CreateLocationRequest r) {
        Warehouse wh = warehouses.findById(r.warehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse", r.warehouseId()));
        if (!wh.isActive()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive: " + wh.getCode());
        }

        String code = Codes.normalize(r.code());
        if (locations.existsByWarehouseIdAndCode(wh.getId(), code)) {
            throw new ConflictException("LOCATION_CODE_EXISTS",
                    "Location code already exists in " + wh.getCode() + ": " + code);
        }

        Location parent = null;
        if (r.parentId() != null) {
            parent = locations.findById(r.parentId())
                    .orElseThrow(() -> new NotFoundException("Location", r.parentId()));
            if (!parent.getWarehouse().getId().equals(wh.getId())) {
                throw new BusinessException("PARENT_OTHER_WAREHOUSE", "Parent belongs to a different warehouse");
            }
            if (!parent.isActive()) {
                throw new BusinessException("PARENT_INACTIVE", "Parent location is inactive");
            }
            if (!r.type().canBeChildOf(parent.getType())) {
                throw new BusinessException("INVALID_HIERARCHY",
                        r.type() + " cannot be placed under " + parent.getType());
            }
        }

        LocationUsage usage = r.usage() == null ? LocationUsage.STORAGE : r.usage();
        validateUsage(r.type(), usage);

        String barcode = Codes.blankToNull(r.barcode());
        if (barcode == null) barcode = wh.getCode() + "-" + code;     // generated default
        assertBarcodeFree(barcode, null);

        Location l = new Location();
        l.setWarehouse(wh);
        l.setParent(parent);
        l.setCode(code);
        l.setName(Codes.blankToNull(r.name()));
        l.setType(r.type());
        l.setUsage(usage);
        l.setBarcode(barcode);
        l.setMaxCapacity(r.maxCapacity());
        return LocationMapper.toResponse(locations.save(l));
    }

    @Override
    public LocationResponse update(UUID id, UpdateLocationRequest r) {
        Location l = find(id);

        LocationUsage usage = r.usage() == null ? l.getUsage() : r.usage();
        validateUsage(l.getType(), usage);

        String barcode = Codes.blankToNull(r.barcode());
        if (barcode != null && !barcode.equals(l.getBarcode())) {
            assertBarcodeFree(barcode, l.getId());
            l.setBarcode(barcode);
        }
        l.setName(Codes.blankToNull(r.name()));
        l.setUsage(usage);
        l.setMaxCapacity(r.maxCapacity());
        return LocationMapper.toResponse(l);
    }

    @Override
    public LocationResponse activate(UUID id) {
        Location l = find(id);
        if (!l.getWarehouse().isActive()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        if (l.getParent() != null && !l.getParent().isActive()) {
            throw new BusinessException("PARENT_INACTIVE", "Activate the parent location first");
        }
        l.setActive(true);
        return LocationMapper.toResponse(l);
    }

    @Override
    public LocationResponse deactivate(UUID id) {
        Location l = find(id);
        if (locations.existsByParentIdAndActiveTrue(id)) {
            throw new ConflictException("HAS_ACTIVE_CHILDREN", "Deactivate the child locations first");
        }
        guards.check(DeactivationGuard.Target.LOCATION, id);       // e.g. inventory: still holds stock
        l.setActive(false);
        return LocationMapper.toResponse(l);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<LocationResponse> search(UUID warehouseId, String q, Pageable pageable) {
        return locations.search(warehouseId, Codes.likePattern(q), pageable).map(LocationMapper::toResponse);
    }

    @Override @Transactional(readOnly = true)
    public List<LocationTreeResponse> tree(UUID warehouseId) {
        List<Location> all = locations.findByWarehouseIdOrderByCode(warehouseId);
        Map<UUID, List<Location>> byParent = all.stream()
                .filter(l -> l.getParent() != null)
                .collect(Collectors.groupingBy(l -> l.getParent().getId()));
        return all.stream()
                .filter(l -> l.getParent() == null)
                .map(l -> LocationMapper.toNode(l, byParent))
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public LocationResponse getById(UUID id) {
        return LocationMapper.toResponse(find(id));
    }

    @Override @Transactional(readOnly = true)
    public LocationResponse getByBarcode(UUID warehouseId, String barcode) {
        return locations.findByWarehouseIdAndBarcode(warehouseId, barcode.trim())
                .map(LocationMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Location barcode", barcode));
    }

    @Override @Transactional(readOnly = true)
    public List<LocationResponse> findBinsByUsage(UUID warehouseId, LocationUsage usage) {
        return locations.findByWarehouseIdAndTypeAndUsageAndActiveTrueOrderByCode(
                        warehouseId, LocationType.BIN, usage)
                .stream().map(LocationMapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public LocationResponse requireStorableBin(UUID id) {
        Location l = find(id);
        if (l.getType() != LocationType.BIN) {
            throw new BusinessException("NOT_A_BIN", "Stock can only be held in BIN locations: " + l.getCode());
        }
        if (!l.isActive() || !l.getWarehouse().isActive()) {
            throw new BusinessException("LOCATION_INACTIVE", "Location is inactive: " + l.getCode());
        }
        return LocationMapper.toResponse(l);
    }

    private Location find(UUID id) {
        return locations.findById(id).orElseThrow(() -> new NotFoundException("Location", id));
    }

    private void validateUsage(LocationType type, LocationUsage usage) {
        if (usage != LocationUsage.STORAGE && type != LocationType.BIN) {
            throw new BusinessException("INVALID_USAGE", "Only BIN locations can have usage " + usage);
        }
    }

    private void assertBarcodeFree(String barcode, UUID selfId) {
        boolean taken = selfId == null
                ? locations.existsByBarcode(barcode)
                : locations.existsByBarcodeAndIdNot(barcode, selfId);
        if (taken) throw new ConflictException("LOCATION_BARCODE_EXISTS", "Barcode already in use: " + barcode);
    }
}

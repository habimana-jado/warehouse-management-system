package rw.afriteck.wms.foundation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.foundation.dtos.CreateLocationRequest;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;
import rw.afriteck.wms.foundation.dtos.UpdateLocationRequest;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LocationService {

    LocationResponse create(CreateLocationRequest r);

    LocationResponse update(UUID id, UpdateLocationRequest r);

    LocationResponse activate(UUID id);

    LocationResponse deactivate(UUID id);

    Page<LocationResponse> search(UUID warehouseId, String q, Pageable pageable);

    List<LocationTreeResponse> tree(UUID warehouseId);

    // Lookups for other modules
    LocationResponse getById(UUID id);

    LocationResponse getByBarcode(UUID warehouseId, String barcode);

    List<LocationResponse> findBinsByUsage(UUID warehouseId, LocationUsage usage);

    List<LocationResponse> findAllByIds(Collection<UUID> ids);

    /**
     * Stock may only be posted to active BIN locations. Throws BusinessException otherwise.
     */
    LocationResponse requireStorableBin(UUID id);
}

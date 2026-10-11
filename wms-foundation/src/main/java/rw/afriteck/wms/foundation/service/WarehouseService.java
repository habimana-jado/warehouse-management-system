package rw.afriteck.wms.foundation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.foundation.dtos.CreateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.UpdateWarehouseRequest;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;

import java.util.List;
import java.util.UUID;

public interface WarehouseService {

    WarehouseResponse create(CreateWarehouseRequest r);

    WarehouseResponse update(UUID id, UpdateWarehouseRequest r);

    WarehouseResponse activate(UUID id);

    WarehouseResponse deactivate(UUID id);

    Page<WarehouseResponse> search(String q, Pageable pageable);

    List<WarehouseResponse> findAccessibleToCurrentUser();

    // Lookups for other modules
    WarehouseResponse getById(UUID id);          // NotFoundException if missing

    boolean exists(UUID id);

    List<WarehouseResponse> findAllActive();
}

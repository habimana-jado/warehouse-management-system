package rw.afriteck.wms.foundation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.foundation.dtos.CreateSupplierRequest;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.UpdateSupplierRequest;

import java.util.List;
import java.util.UUID;

public interface SupplierService {

    SupplierResponse create(CreateSupplierRequest r);

    SupplierResponse update(UUID id, UpdateSupplierRequest r);

    SupplierResponse activate(UUID id);

    SupplierResponse deactivate(UUID id);

    Page<SupplierResponse> search(String q, Pageable pageable);

    SupplierResponse getById(UUID id);            // lookup for inbound

    /** Active suppliers sorted by name (pickers in the UI). */
    List<SupplierResponse> findAllActive();
}

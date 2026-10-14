package rw.afriteck.wms.inventory.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inventory.dtos.CreateReorderPolicyRequest;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;

import java.util.List;
import java.util.UUID;

public interface ReorderPolicyService {

    ReorderPolicyResponse create(CreateReorderPolicyRequest request);

    ReorderPolicyResponse update(UUID id, UpdateReorderPolicyRequest request);

    ReorderPolicyResponse getById(UUID id);

    PageResponse<ReorderPolicyResponse> search(UUID warehouseId, boolean activeOnly, Pageable pageable);

    List<LowStockResponse> findBelowMinimum(UUID warehouseId);        // most critical first; reporting may reuse it

    UUID warehouseIdOf(UUID id);
}

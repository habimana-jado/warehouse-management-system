package rw.afriteck.wms.inventory.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;

import java.util.UUID;

public interface AdjustmentReasonService {

    AdjustmentReasonResponse create(CreateAdjustmentReasonRequest r);

    AdjustmentReasonResponse update(UUID id, UpdateAdjustmentReasonRequest r);

    AdjustmentReasonResponse getById(UUID id);

    PageResponse<AdjustmentReasonResponse> search(String q, boolean includeInactive, Pageable pageable);
}

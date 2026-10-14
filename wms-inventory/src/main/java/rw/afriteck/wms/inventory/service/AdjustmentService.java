package rw.afriteck.wms.inventory.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inventory.dtos.*;

import java.util.UUID;

public interface AdjustmentService {

    AdjustmentResponse create(CreateAdjustmentRequest r, UUID requesterId);

    AdjustmentResponse getById(UUID id);

    PageResponse<AdjustmentSummaryResponse> search(AdjustmentFilter filter, Pageable pageable);

    AdjustmentResponse approve(UUID id, UUID approverId);

    AdjustmentResponse reject(UUID id, UUID rejecterId, String reason);

    AdjustmentResponse cancel(UUID id, UUID actorId);          // requester only

    AdjustmentAccessInfo accessInfo(UUID id);                               // for controller access checks
}

package rw.afriteck.wms.outbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.enums.PickListStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PickListService {

    // ---- reads ----
    PickListResponse getById(UUID id);

    PickListResponse getByTaskId(UUID taskId);                                   // a worker opens the list from their task

    List<PickListResponse> findBySalesOrder(UUID salesOrderId);

    PageResponse<PickListSummaryResponse> search(UUID warehouseId, Set<PickListStatus> statuses, Pageable pageable);

    // ---- outbound's own controllers ----
    PickListResponse release(UUID salesOrderId, ReleaseSalesOrderRequest request);

    PickListResponse confirmPick(UUID pickListId, UUID lineId, ConfirmPickRequest request, UUID actorId, boolean privileged);

    PickListResponse cancel(UUID pickListId, CancelPickListRequest request);

    UUID warehouseIdOf(UUID pickListId);

    // ---- called by SalesOrderService (cancel, close) ----

    /**
     * Joins the caller's transaction; the caller already holds the order lock. See step 5 for the exact behavior.
     */
    void cancelOpenForOrder(UUID salesOrderId, String reason);
}

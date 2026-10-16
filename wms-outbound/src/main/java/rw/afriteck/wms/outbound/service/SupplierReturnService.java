package rw.afriteck.wms.outbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;

import java.util.Set;
import java.util.UUID;

public interface SupplierReturnService {

    // ---- reads ----
    SupplierReturnResponse getById(UUID id);

    PageResponse<SupplierReturnSummaryResponse> search(UUID warehouseId, Set<SupplierReturnStatus> statuses,
                                                       String q, Pageable pageable);

    // ---- outbound's own controllers ----
    SupplierReturnResponse create(CreateSupplierReturnRequest request);             // DRAFT; runs InventoryService.check

    SupplierReturnResponse post(UUID id);                                           // DRAFT -> POSTED, ISSUE lines in the ledger

    SupplierReturnResponse cancel(UUID id, CancelSupplierReturnRequest request);    // DRAFT only

    UUID warehouseIdOf(UUID id);
}

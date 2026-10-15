package rw.afriteck.wms.inbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inbound.dtos.CreateCustomerReturnRequest;
import rw.afriteck.wms.inbound.dtos.CustomerReturnResponse;
import rw.afriteck.wms.inbound.dtos.CustomerReturnSummaryResponse;
import rw.afriteck.wms.inbound.dtos.ReceiveCustomerReturnRequest;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;

import java.util.Set;
import java.util.UUID;

public interface CustomerReturnService {

    /** Registers an expected return (EXPECTED). No stock moves yet. */
    CustomerReturnResponse create(CreateCustomerReturnRequest request);

    /** Goods arrived and were inspected: posts RECEIPT lines (referenceType "RMA") and marks the return RECEIVED. */
    CustomerReturnResponse receive(UUID id, ReceiveCustomerReturnRequest request, UUID actorId);

    /** EXPECTED only. Reason mandatory. */
    CustomerReturnResponse cancel(UUID id, String reason);

    CustomerReturnResponse getById(UUID id);

    /** statuses null or empty = all statuses; q (optional) matches the return number or the sales order number. */
    PageResponse<CustomerReturnSummaryResponse> search(UUID warehouseId, Set<CustomerReturnStatus> statuses,
                                                       String q, Pageable pageable);

    /** For controllers' warehouse access checks. 404 when the return does not exist. */
    UUID warehouseIdOf(UUID id);
}
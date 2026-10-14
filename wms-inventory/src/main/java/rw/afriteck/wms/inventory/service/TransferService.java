package rw.afriteck.wms.inventory.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inventory.dtos.*;

import java.util.UUID;

public interface TransferService {

    TransferResponse create(CreateTransferRequest request);

    TransferResponse getById(UUID id);

    PageResponse<TransferSummaryResponse> search(TransferFilter filter, Pageable pageable);

    TransferResponse ship(UUID id);                                          // INTER_WAREHOUSE: PENDING -> IN_TRANSIT

    TransferResponse receive(UUID id, ReceiveTransferRequest request);       // INTER_WAREHOUSE: IN_TRANSIT -> COMPLETED

    TransferResponse confirmLine(UUID id, UUID lineId, ConfirmTransferLineRequest request,
                                 UUID actorId, boolean privileged);          // planned BIN; privileged = manager/admin may confirm for others

    TransferResponse cancel(UUID id, String reason);

    TransferAccessInfo accessInfo(UUID id);
}

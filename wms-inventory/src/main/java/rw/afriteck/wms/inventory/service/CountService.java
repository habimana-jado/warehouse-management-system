package rw.afriteck.wms.inventory.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inventory.dtos.*;

import java.util.UUID;

public interface CountService {

    CountResponse create(CreateCountRequest request);

    CountResponse start(UUID id, StartCountRequest request);

    CountResponse getById(UUID id);

    PageResponse<CountResponse> search(CountFilter filter, Pageable pageable);

    PageResponse<CountLineResponse> lines(UUID id, boolean onlyVariance, Pageable pageable);

    CountLineResponse recordCount(UUID id, UUID lineId, RecordCountRequest request, UUID actorId, boolean privileged);

    CountLineResponse addLine(UUID id, AddCountLineRequest request, UUID actorId);

    CountResponse submit(UUID id);

    CountResponse reopen(UUID id);

    CountResponse approve(UUID id, UUID approverId);

    CountResponse cancel(UUID id, String reason);

    UUID warehouseIdOf(UUID id);
}

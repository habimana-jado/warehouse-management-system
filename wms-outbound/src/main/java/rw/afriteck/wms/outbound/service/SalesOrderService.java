package rw.afriteck.wms.outbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.outbound.dtos.*;

import java.util.UUID;

public interface SalesOrderService {

    // ---- reads: other modules (reporting) may call ----
    SalesOrderResponse getById(UUID id);

    PageResponse<SalesOrderSummaryResponse> search(SalesOrderFilter filter, Pageable pageable);

    // ---- outbound's own controllers ----
    SalesOrderResponse create(CreateSalesOrderRequest request);

    SalesOrderResponse update(UUID id, UpdateSalesOrderRequest request);          // DRAFT only

    ConfirmSalesOrderResponse confirm(UUID id);                                   // DRAFT -> CONFIRMED, with warnings

    SalesOrderResponse cancel(UUID id, CancelSalesOrderRequest request);          // DRAFT or CONFIRMED, nothing picked

    SalesOrderResponse close(UUID id);                                            // PARTIALLY_SHIPPED only (close short)

    UUID warehouseIdOf(UUID id);                                                  // controllers' warehouse access check
}

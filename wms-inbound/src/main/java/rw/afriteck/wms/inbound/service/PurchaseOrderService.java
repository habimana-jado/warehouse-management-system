package rw.afriteck.wms.inbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inbound.dtos.*;

import java.math.BigDecimal;
import java.util.UUID;

public interface PurchaseOrderService {

    // ---- inbound's own controllers ----
    PurchaseOrderResponse create(CreatePurchaseOrderRequest request);

    /** DRAFT only. */
    PurchaseOrderResponse update(UUID id, UpdatePurchaseOrderRequest request);

    PurchaseOrderResponse confirm(UUID id, UUID actorId);

    /** DRAFT or CONFIRMED (nothing received yet). Reason mandatory. */
    PurchaseOrderResponse cancel(UUID id, String reason);

    /** "Close short": ends a PARTIALLY_RECEIVED PO whose remaining quantity will not arrive. */
    PurchaseOrderResponse close(UUID id, UUID actorId);

    /** Open POs only (CONFIRMED / PARTIALLY_RECEIVED). */
    PurchaseOrderResponse changeTolerance(UUID id, UUID lineId, BigDecimal tolerancePercent);

    PurchaseOrderResponse getById(UUID id);

    PageResponse<PurchaseOrderSummaryResponse> search(PurchaseOrderFilter filter, Pageable pageable);

    /** Receiver's handheld: resolve an item or box barcode to the PO line it belongs to. */
    PurchaseOrderLineScanResponse scan(UUID id, String barcode);

    /** For controllers' warehouse access checks. 404 when the PO does not exist. */
    UUID warehouseIdOf(UUID id);
}
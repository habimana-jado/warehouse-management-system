package rw.afriteck.wms.outbound.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.outbound.dtos.ConfirmDeliveryRequest;
import rw.afriteck.wms.outbound.dtos.CreateShipmentRequest;
import rw.afriteck.wms.outbound.dtos.ShipmentResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ShipmentService {

    // ---- reads ----
    ShipmentResponse getById(UUID id);

    List<ShipmentSummaryResponse> findBySalesOrder(UUID salesOrderId);

    PageResponse<ShipmentSummaryResponse> search(UUID warehouseId, String q, Pageable pageable);

    // ---- outbound's own controllers ----
    ShipmentResponse create(UUID salesOrderId, CreateShipmentRequest request);

    ShipmentResponse confirmDelivery(UUID shipmentId, ConfirmDeliveryRequest request);

    UUID warehouseIdOf(UUID shipmentId);
}

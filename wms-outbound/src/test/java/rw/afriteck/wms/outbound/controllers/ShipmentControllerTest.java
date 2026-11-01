package rw.afriteck.wms.outbound.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.dtos.ConfirmDeliveryRequest;
import rw.afriteck.wms.outbound.dtos.ShipmentResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentSummaryResponse;
import rw.afriteck.wms.outbound.enums.ShipmentStatus;
import rw.afriteck.wms.outbound.service.ShipmentService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentControllerTest {

    @Mock
    private ShipmentService shipments;
    @Mock
    private UserContext userContext;

    @InjectMocks
    private ShipmentController controller;

    private UUID shipmentId;
    private UUID warehouseId;
    private ShipmentResponse dummyResponse;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();

        dummyResponse = new ShipmentResponse(
                shipmentId, 1L, "DN-000001", UUID.randomUUID(), "SO-000001", UUID.randomUUID(), "Customer 1",
                warehouseId, ShipmentStatus.DISPATCHED, Instant.now(), "Addr", "DHL", "TRACK123", 2,
                null, null, null, null, Instant.now(), List.of()
        );
    }

    @Test
    void search_CallsServiceAndChecksAccess() {
        when(shipments.search(eq(warehouseId), any(), any(Pageable.class)))
                .thenReturn(PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of())));

        PageResponse<ShipmentSummaryResponse> res = controller.search(warehouseId, "DN", Pageable.unpaged());

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(shipments).search(eq(warehouseId), eq("DN"), any(Pageable.class));
    }

    @Test
    void get_CallsServiceAndChecksAccess() {
        when(shipments.warehouseIdOf(shipmentId)).thenReturn(warehouseId);
        when(shipments.getById(shipmentId)).thenReturn(dummyResponse);

        ShipmentResponse res = controller.get(shipmentId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(shipments).getById(shipmentId);
    }

    @Test
    void confirmDelivery_CallsServiceAndChecksAccess() {
        ConfirmDeliveryRequest req = new ConfirmDeliveryRequest(Instant.now(), "John Doe", "POD Notes");
        when(shipments.warehouseIdOf(shipmentId)).thenReturn(warehouseId);
        when(shipments.confirmDelivery(shipmentId, req)).thenReturn(dummyResponse);

        ShipmentResponse res = controller.confirmDelivery(shipmentId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(shipments).confirmDelivery(shipmentId, req);
    }
}

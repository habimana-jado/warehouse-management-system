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
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.outbound.service.ShipmentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderControllerTest {

    @Mock
    private SalesOrderService salesOrders;
    @Mock
    private PickListService pickLists;
    @Mock
    private ShipmentService shipments;
    @Mock
    private UserContext userContext;

    @InjectMocks
    private SalesOrderController controller;

    private UUID orderId;
    private UUID warehouseId;
    private UUID customerId;
    private SalesOrderResponse dummyResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        dummyResponse = new SalesOrderResponse(
                orderId, "SO-000001", customerId, "Customer 1", warehouseId, SalesOrderStatus.DRAFT,
                LocalDate.now(), null, "USD", null, null, null, null,
                List.of(), BigDecimal.TEN, BigDecimal.valueOf(500), Instant.now(), null
        );
    }

    @Test
    void create_CallsServiceAndChecksAccess() {
        CreateSalesOrderRequest req = new CreateSalesOrderRequest(customerId, warehouseId, LocalDate.now(), null, null, "USD", null, List.of());
        when(salesOrders.create(req)).thenReturn(dummyResponse);

        SalesOrderResponse res = controller.create(req);

        assertNotNull(res);
        assertEquals(orderId, res.id());
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).create(req);
    }

    @Test
    void search_CallsServiceAndChecksAccess() {
        PageResponse<SalesOrderSummaryResponse> pageRes = PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of()));
        when(salesOrders.search(any(SalesOrderFilter.class), any(Pageable.class))).thenReturn(pageRes);

        PageResponse<SalesOrderSummaryResponse> res = controller.search(
                warehouseId, null, null, null, null, null, null, Pageable.unpaged());

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).search(any(SalesOrderFilter.class), any(Pageable.class));
    }

    @Test
    void get_CallsServiceAndChecksAccess() {
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);
        when(salesOrders.getById(orderId)).thenReturn(dummyResponse);

        SalesOrderResponse res = controller.get(orderId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).getById(orderId);
    }

    @Test
    void update_CallsServiceAndChecksAccess() {
        UpdateSalesOrderRequest req = new UpdateSalesOrderRequest(null, null, "REF", "USD", null, List.of());
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);
        when(salesOrders.update(orderId, req)).thenReturn(dummyResponse);

        SalesOrderResponse res = controller.update(orderId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).update(orderId, req);
    }

    @Test
    void confirm_CallsServiceAndChecksAccess() {
        ConfirmSalesOrderResponse confirmRes = new ConfirmSalesOrderResponse(dummyResponse, List.of());
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);
        when(salesOrders.confirm(orderId)).thenReturn(confirmRes);

        ConfirmSalesOrderResponse res = controller.confirm(orderId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).confirm(orderId);
    }

    @Test
    void cancel_CallsServiceAndChecksAccess() {
        CancelSalesOrderRequest req = new CancelSalesOrderRequest("Cancelled");
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);
        when(salesOrders.cancel(orderId, req)).thenReturn(dummyResponse);

        SalesOrderResponse res = controller.cancel(orderId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).cancel(orderId, req);
    }

    @Test
    void close_CallsServiceAndChecksAccess() {
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);
        when(salesOrders.close(orderId)).thenReturn(dummyResponse);

        SalesOrderResponse res = controller.close(orderId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(salesOrders).close(orderId);
    }

    @Test
    void release_CallsPickListService() {
        ReleaseSalesOrderRequest req = new ReleaseSalesOrderRequest(null, null, "Notes");
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);

        controller.release(orderId, req);

        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).release(orderId, req);
    }

    @Test
    void pickLists_CallsPickListService() {
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);

        controller.pickLists(orderId);

        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).findBySalesOrder(orderId);
    }

    @Test
    void ship_CallsShipmentService() {
        CreateShipmentRequest req = new CreateShipmentRequest(null, null, null, null, null, List.of());
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);

        controller.ship(orderId, req);

        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(shipments).create(orderId, req);
    }

    @Test
    void shipments_CallsShipmentService() {
        when(salesOrders.warehouseIdOf(orderId)).thenReturn(warehouseId);

        controller.shipments(orderId);

        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(shipments).findBySalesOrder(orderId);
    }
}

package rw.afriteck.wms.inbound.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderControllerTest {

    @Mock
    private PurchaseOrderService purchaseOrders;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private PurchaseOrderController controller;

    private UUID warehouseId;
    private UUID poId;
    private UUID supplierId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        poId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private PurchaseOrderResponse createSampleResponse(PurchaseOrderStatus status) {
        return new PurchaseOrderResponse(
                poId, 1L, "PO-001", supplierId, "Supplier", warehouseId, status,
                LocalDate.now(), null, "REF1", "USD", "Notes",
                null, null, null, null, null, null,
                null, null, Instant.now(), BigDecimal.valueOf(100), List.of());
    }

    @Test
    void create_Success() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest(
                supplierId, warehouseId, LocalDate.now(), null, "REF1", "USD", null, List.of());
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.DRAFT);

        when(purchaseOrders.create(req)).thenReturn(expectedResponse);

        ResponseEntity<PurchaseOrderResponse> res = controller.create(req);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(expectedResponse, res.getBody());
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<PurchaseOrderSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);

        when(purchaseOrders.search(any(PurchaseOrderFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<PurchaseOrderSummaryResponse> res = controller.search(
                warehouseId, null, null, null, null, null, null, pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Success() {
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.DRAFT);

        when(purchaseOrders.getById(poId)).thenReturn(expectedResponse);

        PurchaseOrderResponse res = controller.get(poId);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void update_Success() {
        UpdatePurchaseOrderRequest req = new UpdatePurchaseOrderRequest(null, "REF2", "USD", null, List.of());
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.DRAFT);

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(purchaseOrders.update(poId, req)).thenReturn(expectedResponse);

        PurchaseOrderResponse res = controller.update(poId, req);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void confirm_Success() {
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.CONFIRMED);

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(purchaseOrders.confirm(poId, userId)).thenReturn(expectedResponse);

        PurchaseOrderResponse res = controller.confirm(poId);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void cancel_Success() {
        CancelPurchaseOrderRequest req = new CancelPurchaseOrderRequest("Reason");
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.CANCELLED);

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(purchaseOrders.cancel(poId, "Reason")).thenReturn(expectedResponse);

        PurchaseOrderResponse res = controller.cancel(poId, req);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void close_Success() {
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.CLOSED);

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(purchaseOrders.close(poId, userId)).thenReturn(expectedResponse);

        PurchaseOrderResponse res = controller.close(poId);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void changeTolerance_Success() {
        UUID lineId = UUID.randomUUID();
        ChangeToleranceRequest req = new ChangeToleranceRequest(BigDecimal.valueOf(5));
        PurchaseOrderResponse expectedResponse = createSampleResponse(PurchaseOrderStatus.CONFIRMED);

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(purchaseOrders.changeTolerance(poId, lineId, BigDecimal.valueOf(5))).thenReturn(expectedResponse);

        PurchaseOrderResponse res = controller.changeTolerance(poId, lineId, req);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void scan_Success() {
        String barcode = "BAR123";
        PurchaseOrderLineScanResponse expectedResponse = new PurchaseOrderLineScanResponse(
                null, "PCS", BigDecimal.ONE);

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(purchaseOrders.scan(poId, barcode)).thenReturn(expectedResponse);

        PurchaseOrderLineScanResponse res = controller.scan(poId, barcode);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}

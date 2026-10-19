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
import rw.afriteck.wms.inbound.enums.ReceiptMode;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoodsReceiptControllerTest {

    @Mock
    private GoodsReceiptService receipts;

    @Mock
    private PurchaseOrderService purchaseOrders;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private GoodsReceiptController controller;

    private UUID poId;
    private UUID receiptId;
    private UUID warehouseId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        poId = UUID.randomUUID();
        receiptId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private GoodsReceiptResponse createSampleResponse() {
        return new GoodsReceiptResponse(
                receiptId, 1L, "GRN-001", poId, "PO-001", warehouseId, ReceiptMode.PUTAWAY,
                Instant.now(), userId, "Worker", "DN1", "Notes", null, List.of());
    }

    @Test
    void receive_Success() {
        ReceiveGoodsRequest req = new ReceiveGoodsRequest(ReceiptMode.PUTAWAY, null, null, "DN1", "Notes", List.of());
        GoodsReceiptResponse expectedResponse = createSampleResponse();

        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(receipts.receive(poId, req, userId)).thenReturn(expectedResponse);

        ResponseEntity<GoodsReceiptResponse> res = controller.receive(poId, req);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals(expectedResponse, res.getBody());
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void listForPurchaseOrder_Success() {
        when(purchaseOrders.warehouseIdOf(poId)).thenReturn(warehouseId);
        when(receipts.findByPurchaseOrder(poId)).thenReturn(List.of());

        List<GoodsReceiptSummaryResponse> res = controller.listForPurchaseOrder(poId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<GoodsReceiptSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);

        when(receipts.search(warehouseId, "search", pageable)).thenReturn(pageResponse);

        PageResponse<GoodsReceiptSummaryResponse> res = controller.search(warehouseId, "search", pageable);

        assertEquals(pageResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void awaitingPutaway_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<GoodsReceiptSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);

        when(receipts.awaitingPutaway(warehouseId, pageable)).thenReturn(pageResponse);

        PageResponse<GoodsReceiptSummaryResponse> res = controller.awaitingPutaway(warehouseId, pageable);

        assertEquals(pageResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Success() {
        GoodsReceiptResponse expectedResponse = createSampleResponse();

        when(receipts.getById(receiptId)).thenReturn(expectedResponse);

        GoodsReceiptResponse res = controller.get(receiptId);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void confirmPutaway_Success() {
        UUID lineId = UUID.randomUUID();
        ConfirmPutawayRequest req = new ConfirmPutawayRequest(UUID.randomUUID(), null, null);
        GoodsReceiptResponse expectedResponse = createSampleResponse();

        when(receipts.warehouseIdOf(receiptId)).thenReturn(warehouseId);
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.hasRole("WAREHOUSE_MANAGER")).thenReturn(true);
        when(userContext.currentUserId()).thenReturn(userId);
        when(receipts.confirmPutaway(receiptId, lineId, req, userId, true)).thenReturn(expectedResponse);

        GoodsReceiptResponse res = controller.confirmPutaway(receiptId, lineId, req);

        assertEquals(expectedResponse, res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}

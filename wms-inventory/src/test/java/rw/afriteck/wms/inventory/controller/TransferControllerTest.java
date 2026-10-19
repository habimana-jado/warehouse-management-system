package rw.afriteck.wms.inventory.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.inventory.service.TransferService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService service;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private TransferController controller;

    private UUID sourceWhId;
    private UUID destWhId;
    private UUID transferId;
    private UUID userId;
    private TransferResponse response;
    private TransferAccessInfo accessInfo;

    @BeforeEach
    void setUp() {
        sourceWhId = UUID.randomUUID();
        destWhId = UUID.randomUUID();
        transferId = UUID.randomUUID();
        userId = UUID.randomUUID();

        response = new TransferResponse(transferId, 1L, "TRF-001", TransferType.INTER_WAREHOUSE, TransferStatus.PENDING,
                sourceWhId, destWhId, null, null, null, "Notes", Instant.now(), List.of());
        accessInfo = new TransferAccessInfo(sourceWhId, destWhId, TransferType.INTER_WAREHOUSE);
    }

    @Test
    void create_ImmediateBin_Success() {
        CreateTransferRequest req = new CreateTransferRequest(TransferType.BIN, sourceWhId, sourceWhId, true, null, null, "Notes", List.of());
        when(service.create(req)).thenReturn(response);

        TransferResponse res = controller.create(req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(sourceWhId);
        verify(service).create(req);
    }

    @Test
    void create_NonImmediate_WorkerForbidden() {
        CreateTransferRequest req = new CreateTransferRequest(TransferType.INTER_WAREHOUSE, sourceWhId, destWhId, false, null, null, "Notes", List.of());
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.hasRole("WAREHOUSE_MANAGER")).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> controller.create(req));
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<TransferSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(service.search(any(TransferFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<TransferSummaryResponse> res = controller.search(sourceWhId, null, "q", pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(sourceWhId);
    }

    @Test
    void get_Success() {
        when(service.accessInfo(transferId)).thenReturn(accessInfo);
        when(userContext.canAccessWarehouse(sourceWhId)).thenReturn(true);
        when(service.getById(transferId)).thenReturn(response);

        TransferResponse res = controller.get(transferId);

        assertNotNull(res);
    }

    @Test
    void ship_Success() {
        when(service.accessInfo(transferId)).thenReturn(accessInfo);
        when(service.ship(transferId)).thenReturn(response);

        TransferResponse res = controller.ship(transferId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(sourceWhId);
    }

    @Test
    void receive_Success() {
        ReceiveTransferRequest req = new ReceiveTransferRequest(List.of(), false, null);
        when(service.accessInfo(transferId)).thenReturn(accessInfo);
        when(service.receive(transferId, req)).thenReturn(response);

        TransferResponse res = controller.receive(transferId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(destWhId);
    }

    @Test
    void confirmLine_Success() {
        UUID lineId = UUID.randomUUID();
        ConfirmTransferLineRequest req = new ConfirmTransferLineRequest(BigDecimal.TEN, null);
        when(service.accessInfo(transferId)).thenReturn(accessInfo);
        when(userContext.currentUserId()).thenReturn(userId);
        when(userContext.isAdmin()).thenReturn(true);
        when(service.confirmLine(transferId, lineId, req, userId, true)).thenReturn(response);

        TransferResponse res = controller.confirmLine(transferId, lineId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(sourceWhId);
    }

    @Test
    void cancel_Success() {
        CancelTransferRequest req = new CancelTransferRequest("Cancelled");
        when(service.accessInfo(transferId)).thenReturn(accessInfo);
        when(userContext.canAccessWarehouse(sourceWhId)).thenReturn(true);
        when(service.cancel(transferId, "Cancelled")).thenReturn(response);

        TransferResponse res = controller.cancel(transferId, req);

        assertNotNull(res);
    }
}

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
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.service.AdjustmentService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentControllerTest {

    @Mock
    private AdjustmentService service;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private AdjustmentController controller;

    private UUID warehouseId;
    private UUID userId;
    private UUID adjId;
    private AdjustmentResponse response;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        userId = UUID.randomUUID();
        adjId = UUID.randomUUID();

        response = new AdjustmentResponse(adjId, 1L, "ADJ-001", warehouseId, UUID.randomUUID(), "CORRECTION", "Correction",
                AdjustmentStatus.PENDING_APPROVAL, "Notes", userId, "User", null, null, null, null, Instant.now(), List.of());
    }

    @Test
    void create_Success() {
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, UUID.randomUUID(), "Notes", List.of());
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.create(req, userId)).thenReturn(response);

        AdjustmentResponse res = controller.create(req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).create(req, userId);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<AdjustmentSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(service.search(any(AdjustmentFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<AdjustmentSummaryResponse> res = controller.search(warehouseId, null, "q", pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void mine_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<AdjustmentSummaryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.search(any(AdjustmentFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<AdjustmentSummaryResponse> res = controller.mine(warehouseId, null, "q", pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Success_WorkerOwnAdjustment() {
        AdjustmentAccessInfo info = new AdjustmentAccessInfo(warehouseId, userId);
        when(service.accessInfo(adjId)).thenReturn(info);
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.hasRole("WAREHOUSE_MANAGER")).thenReturn(false);
        when(userContext.hasRole("VIEWER")).thenReturn(false);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.getById(adjId)).thenReturn(response);

        AdjustmentResponse res = controller.get(adjId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Forbidden_WorkerOtherUserAdjustment() {
        AdjustmentAccessInfo info = new AdjustmentAccessInfo(warehouseId, UUID.randomUUID());
        when(service.accessInfo(adjId)).thenReturn(info);
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.hasRole("WAREHOUSE_MANAGER")).thenReturn(false);
        when(userContext.hasRole("VIEWER")).thenReturn(false);
        when(userContext.currentUserId()).thenReturn(userId);

        assertThrows(ForbiddenException.class, () -> controller.get(adjId));
    }

    @Test
    void approve_Success() {
        AdjustmentAccessInfo info = new AdjustmentAccessInfo(warehouseId, userId);
        when(service.accessInfo(adjId)).thenReturn(info);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.approve(adjId, userId)).thenReturn(response);

        AdjustmentResponse res = controller.approve(adjId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void reject_Success() {
        AdjustmentAccessInfo info = new AdjustmentAccessInfo(warehouseId, userId);
        when(service.accessInfo(adjId)).thenReturn(info);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.reject(adjId, userId, "Bad")).thenReturn(response);

        AdjustmentResponse res = controller.reject(adjId, new RejectAdjustmentRequest("Bad"));

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void cancel_Success() {
        AdjustmentAccessInfo info = new AdjustmentAccessInfo(warehouseId, userId);
        when(service.accessInfo(adjId)).thenReturn(info);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.cancel(adjId, userId)).thenReturn(response);

        AdjustmentResponse res = controller.cancel(adjId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}

package rw.afriteck.wms.outbound.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.dtos.TaskAccessInfo;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.outbound.dtos.CancelPickListRequest;
import rw.afriteck.wms.outbound.dtos.ConfirmPickRequest;
import rw.afriteck.wms.outbound.dtos.PickListResponse;
import rw.afriteck.wms.outbound.dtos.PickListSummaryResponse;
import rw.afriteck.wms.outbound.enums.PickListStatus;
import rw.afriteck.wms.outbound.service.PickListService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickListControllerTest {

    @Mock
    private PickListService pickLists;
    @Mock
    private TaskService taskService;
    @Mock
    private UserContext userContext;

    @InjectMocks
    private PickListController controller;

    private UUID pickListId;
    private UUID warehouseId;
    private UUID taskId;
    private UUID userId;
    private PickListResponse dummyResponse;

    @BeforeEach
    void setUp() {
        pickListId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        userId = UUID.randomUUID();

        dummyResponse = new PickListResponse(
                pickListId, 1L, "PCK-000001", UUID.randomUUID(), "SO-000001", UUID.randomUUID(), "Customer 1",
                warehouseId, PickListStatus.RELEASED, taskId, Instant.now(), null, null, List.of()
        );
    }

    @Test
    void search_CallsServiceAndChecksAccess() {
        when(pickLists.search(eq(warehouseId), any(), any(Pageable.class)))
                .thenReturn(PageResponse.of(new org.springframework.data.domain.PageImpl<>(List.of())));

        PageResponse<PickListSummaryResponse> res = controller.search(warehouseId, null, Pageable.unpaged());

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).search(eq(warehouseId), any(), any(Pageable.class));
    }

    @Test
    void get_PrivilegedUser_ReturnsPickList() {
        when(pickLists.warehouseIdOf(pickListId)).thenReturn(warehouseId);
        when(pickLists.getById(pickListId)).thenReturn(dummyResponse);
        when(userContext.isAdmin()).thenReturn(true);

        PickListResponse res = controller.get(pickListId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).getById(pickListId);
    }

    @Test
    void get_PickerAssigned_ReturnsPickList() {
        when(pickLists.warehouseIdOf(pickListId)).thenReturn(warehouseId);
        when(pickLists.getById(pickListId)).thenReturn(dummyResponse);
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.hasRole("WAREHOUSE_MANAGER")).thenReturn(false);
        when(userContext.hasRole("VIEWER")).thenReturn(false);
        when(userContext.currentUserId()).thenReturn(userId);
        when(taskService.accessInfo(taskId)).thenReturn(new TaskAccessInfo(warehouseId, userId));

        PickListResponse res = controller.get(pickListId);

        assertNotNull(res);
    }

    @Test
    void get_PickerNotAssigned_ThrowsForbiddenException() {
        UUID otherUserId = UUID.randomUUID();
        when(pickLists.warehouseIdOf(pickListId)).thenReturn(warehouseId);
        when(pickLists.getById(pickListId)).thenReturn(dummyResponse);
        when(userContext.isAdmin()).thenReturn(false);
        when(userContext.hasRole("WAREHOUSE_MANAGER")).thenReturn(false);
        when(userContext.hasRole("VIEWER")).thenReturn(false);
        when(userContext.currentUserId()).thenReturn(userId);
        when(taskService.accessInfo(taskId)).thenReturn(new TaskAccessInfo(warehouseId, otherUserId));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> controller.get(pickListId));
        assertEquals("This pick list is assigned to another user", ex.getMessage());
    }

    @Test
    void byTask_CallsServiceAndChecksAccess() {
        when(pickLists.getByTaskId(taskId)).thenReturn(dummyResponse);
        when(userContext.isAdmin()).thenReturn(true);

        PickListResponse res = controller.byTask(taskId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).getByTaskId(taskId);
    }

    @Test
    void confirm_CallsServiceAndChecksAccess() {
        UUID lineId = UUID.randomUUID();
        ConfirmPickRequest req = new ConfirmPickRequest(List.of(), null, false);
        when(pickLists.warehouseIdOf(pickListId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(userContext.isAdmin()).thenReturn(true);
        when(pickLists.confirmPick(eq(pickListId), eq(lineId), eq(req), eq(userId), eq(true)))
                .thenReturn(dummyResponse);

        PickListResponse res = controller.confirm(pickListId, lineId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).confirmPick(eq(pickListId), eq(lineId), eq(req), eq(userId), eq(true));
    }

    @Test
    void cancel_CallsServiceAndChecksAccess() {
        CancelPickListRequest req = new CancelPickListRequest("Cancelled");
        when(pickLists.warehouseIdOf(pickListId)).thenReturn(warehouseId);
        when(pickLists.cancel(pickListId, req)).thenReturn(dummyResponse);

        PickListResponse res = controller.cancel(pickListId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(pickLists).cancel(pickListId, req);
    }
}

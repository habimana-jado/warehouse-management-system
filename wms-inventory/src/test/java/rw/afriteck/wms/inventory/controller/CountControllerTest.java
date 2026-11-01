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
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.enums.CountLineStatus;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.service.CountService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountControllerTest {

    @Mock
    private CountService service;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private CountController controller;

    private UUID warehouseId;
    private UUID countId;
    private UUID userId;
    private CountResponse countResponse;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        countId = UUID.randomUUID();
        userId = UUID.randomUUID();

        countResponse = new CountResponse(countId, 1L, "CNT-001", warehouseId, null, CountStatus.DRAFT, null, null, null, null, null, "Notes", Instant.now(), 0L, 0L, 0L);
    }

    @Test
    void create_Success() {
        CreateCountRequest req = new CreateCountRequest(warehouseId, null, "Notes");
        when(service.create(req)).thenReturn(countResponse);

        CountResponse res = controller.create(req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).create(req);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<CountResponse> pageResponse = new PageResponse<>(List.of(countResponse), 0, 10, 1, 1);
        when(service.search(any(CountFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<CountResponse> res = controller.search(warehouseId, null, "q", pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Success() {
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(service.getById(countId)).thenReturn(countResponse);

        CountResponse res = controller.get(countId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void lines_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<CountLineResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(service.lines(countId, false, pageable)).thenReturn(pageResponse);

        PageResponse<CountLineResponse> res = controller.lines(countId, false, pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void start_Success() {
        StartCountRequest req = new StartCountRequest(null, null);
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(service.start(countId, req)).thenReturn(countResponse);

        CountResponse res = controller.start(countId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void recordCount_Success() {
        UUID lineId = UUID.randomUUID();
        RecordCountRequest req = new RecordCountRequest(BigDecimal.TEN);
        CountLineResponse lineRes = new CountLineResponse(lineId, 1, UUID.randomUUID(), "BIN1", UUID.randomUUID(), "SKU1", "Item 1", null, null, null, null, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, CountLineStatus.COUNTED, userId, "User", Instant.now());

        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(userContext.isAdmin()).thenReturn(true);
        when(service.recordCount(countId, lineId, req, userId, true)).thenReturn(lineRes);

        CountLineResponse res = controller.recordCount(countId, lineId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void addLine_Success() {
        AddCountLineRequest req = new AddCountLineRequest(UUID.randomUUID(), UUID.randomUUID(), null, null, BigDecimal.TEN);
        CountLineResponse lineRes = new CountLineResponse(UUID.randomUUID(), 1, UUID.randomUUID(), "BIN1", UUID.randomUUID(), "SKU1", "Item 1", null, null, null, null, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, CountLineStatus.COUNTED, userId, "User", Instant.now());

        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.addLine(countId, req, userId)).thenReturn(lineRes);

        CountLineResponse res = controller.addLine(countId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void submit_Success() {
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(service.submit(countId)).thenReturn(countResponse);

        CountResponse res = controller.submit(countId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void reopen_Success() {
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(service.reopen(countId)).thenReturn(countResponse);

        CountResponse res = controller.reopen(countId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void approve_Success() {
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(userContext.currentUserId()).thenReturn(userId);
        when(service.approve(countId, userId)).thenReturn(countResponse);

        CountResponse res = controller.approve(countId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void cancel_Success() {
        CancelCountRequest req = new CancelCountRequest("Cancelled");
        when(service.warehouseIdOf(countId)).thenReturn(warehouseId);
        when(service.cancel(countId, "Cancelled")).thenReturn(countResponse);

        CountResponse res = controller.cancel(countId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}

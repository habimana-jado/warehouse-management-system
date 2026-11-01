package rw.afriteck.wms.inventory.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;
import rw.afriteck.wms.inventory.entity.StockAdjustment;
import rw.afriteck.wms.inventory.entity.StockAdjustmentLine;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.repository.AdjustmentReasonRepository;
import rw.afriteck.wms.inventory.repository.StockAdjustmentRepository;
import rw.afriteck.wms.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustmentServiceImplTest {

    @Mock
    private StockAdjustmentRepository adjustments;
    @Mock
    private AdjustmentReasonRepository reasons;
    @Mock
    private InventoryService inventory;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private ItemService itemService;
    @Mock
    private LocationService locationService;
    @Mock
    private WorkerLookup workers;
    @Mock
    private Clock clock;

    @InjectMocks
    private AdjustmentServiceImpl service;

    private UUID warehouseId;
    private UUID itemId;
    private UUID locationId;
    private UUID reasonId;
    private UUID requesterId;
    private UUID approverId;
    private WarehouseResponse warehouse;
    private AdjustmentReason reason;
    private ItemResponse item;
    private LocationResponse location;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();
        reasonId = UUID.randomUUID();
        requesterId = UUID.randomUUID();
        approverId = UUID.randomUUID();

        warehouse = new WarehouseResponse(warehouseId, "WH1", "Main Warehouse", "Addr", "City", "Country", true);
        reason = new AdjustmentReason();
        reason.setId(reasonId);
        reason.setCode("CORRECTION");
        reason.setName("Correction");
        reason.setDirection(AdjustmentDirection.BOTH);
        reason.setRequiresApproval(true);
        reason.setActive(true);

        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.NONE, false, true);
        location = new LocationResponse(locationId, warehouseId, null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
    }

    @Test
    void create_Success_RequiresApproval() {
        AdjustmentLineRequest lineReq = new AdjustmentLineRequest(locationId, itemId, null, null, null, BigDecimal.TEN);
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, reasonId, "Notes", List.of(lineReq));

        when(warehouseService.getById(any())).thenReturn(warehouse);
        when(reasons.findById(any())).thenReturn(Optional.of(reason));
        when(itemService.findAllByIds(any())).thenReturn(List.of(item));
        when(locationService.requireStorableBin(any())).thenReturn(location);
        when(adjustments.nextNumber()).thenReturn(100L);

        StockAdjustment savedAdj = new StockAdjustment();
        savedAdj.setId(UUID.randomUUID());
        savedAdj.setWarehouseId(warehouseId);
        savedAdj.setReason(reason);
        savedAdj.setRequestedBy(requesterId);
        savedAdj.setStatus(AdjustmentStatus.PENDING_APPROVAL);
        savedAdj.setAdjustmentNumber("ADJ-000100");

        StockAdjustmentLine line = new StockAdjustmentLine();
        line.setAdjustment(savedAdj);
        line.setItemId(itemId);
        line.setLocationId(locationId);
        line.setQuantityDelta(BigDecimal.TEN);
        savedAdj.getLines().add(line);

        when(adjustments.save(any(StockAdjustment.class))).thenReturn(savedAdj);
        when(adjustments.saveAndFlush(any(StockAdjustment.class))).thenReturn(savedAdj);
        when(workers.findDisplayNames(anySet())).thenReturn(Map.of(requesterId, "Requester Name"));
        when(locationService.findAllByIds(any())).thenReturn(List.of(location));

        AdjustmentResponse res = service.create(req, requesterId);

        assertNotNull(res);
        verify(inventory).check(any(PostStockRequest.class));
        verify(adjustments).saveAndFlush(any(StockAdjustment.class));
    }

    @Test
    void create_Success_AutoPost_WhenApprovalNotRequired() {
        reason.setRequiresApproval(false);
        AdjustmentLineRequest lineReq = new AdjustmentLineRequest(locationId, itemId, null, null, null, BigDecimal.TEN);
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, reasonId, "Notes", List.of(lineReq));

        when(warehouseService.getById(any())).thenReturn(warehouse);
        when(reasons.findById(any())).thenReturn(Optional.of(reason));
        when(itemService.findAllByIds(any())).thenReturn(List.of(item));
        when(locationService.requireStorableBin(any())).thenReturn(location);
        when(adjustments.nextNumber()).thenReturn(101L);
        when(clock.instant()).thenReturn(Instant.now());

        StockAdjustment savedAdj = new StockAdjustment();
        savedAdj.setId(UUID.randomUUID());
        savedAdj.setWarehouseId(warehouseId);
        savedAdj.setReason(reason);
        savedAdj.setRequestedBy(requesterId);
        savedAdj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        StockAdjustmentLine line = new StockAdjustmentLine();
        line.setAdjustment(savedAdj);
        line.setItemId(itemId);
        line.setLocationId(locationId);
        line.setQuantityDelta(BigDecimal.TEN);
        savedAdj.getLines().add(line);

        when(adjustments.save(any(StockAdjustment.class))).thenReturn(savedAdj);
        when(adjustments.saveAndFlush(any(StockAdjustment.class))).thenReturn(savedAdj);
        when(locationService.findAllByIds(any())).thenReturn(List.of(location));

        AdjustmentResponse res = service.create(req, requesterId);

        assertNotNull(res);
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void create_NullActor_ThrowsBusinessException() {
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, reasonId, "Notes", List.of());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req, null));
        assertEquals("ACTOR_REQUIRED", ex.getCode());
    }

    @Test
    void create_NoLines_ThrowsBusinessException() {
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, reasonId, "Notes", List.of());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req, requesterId));
        assertEquals("ADJUSTMENT_NO_LINES", ex.getCode());
    }

    @Test
    void create_InactiveWarehouse_ThrowsBusinessException() {
        WarehouseResponse inactiveWarehouse = new WarehouseResponse(warehouseId, "WH1", "Main Warehouse", "Addr", "City", "Country", false);
        AdjustmentLineRequest lineReq = new AdjustmentLineRequest(locationId, itemId, null, null, null, BigDecimal.TEN);
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, reasonId, "Notes", List.of(lineReq));

        when(warehouseService.getById(warehouseId)).thenReturn(inactiveWarehouse);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req, requesterId));
        assertEquals("WAREHOUSE_INACTIVE", ex.getCode());
    }

    @Test
    void create_InvalidDirection_ThrowsBusinessException() {
        reason.setDirection(AdjustmentDirection.INCREASE);
        AdjustmentLineRequest lineReq = new AdjustmentLineRequest(locationId, itemId, null, null, null, BigDecimal.TEN.negate());
        CreateAdjustmentRequest req = new CreateAdjustmentRequest(warehouseId, reasonId, "Notes", List.of(lineReq));

        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(reasons.findById(reasonId)).thenReturn(Optional.of(reason));
        when(itemService.findAllByIds(any())).thenReturn(List.of(item));
        when(locationService.requireStorableBin(any())).thenReturn(location);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req, requesterId));
        assertEquals("ADJUSTMENT_DIRECTION_INVALID", ex.getCode());
    }

    @Test
    void approve_Success() {
        UUID adjId = UUID.randomUUID();
        StockAdjustment adj = new StockAdjustment();
        adj.setId(adjId);
        adj.setWarehouseId(warehouseId);
        adj.setRequestedBy(requesterId);
        adj.setReason(reason);
        adj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        when(adjustments.findWithLinesById(adjId)).thenReturn(Optional.of(adj));
        when(clock.instant()).thenReturn(Instant.now());
        when(adjustments.saveAndFlush(adj)).thenReturn(adj);

        AdjustmentResponse res = service.approve(adjId, approverId);

        assertNotNull(res);
        assertEquals(AdjustmentStatus.POSTED, adj.getStatus());
        assertEquals(approverId, adj.getDecidedBy());
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void approve_SelfApproval_ThrowsBusinessException() {
        UUID adjId = UUID.randomUUID();
        StockAdjustment adj = new StockAdjustment();
        adj.setId(adjId);
        adj.setRequestedBy(requesterId);
        adj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        when(adjustments.findWithLinesById(adjId)).thenReturn(Optional.of(adj));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approve(adjId, requesterId));
        assertEquals("SELF_APPROVAL_NOT_ALLOWED", ex.getCode());
    }

    @Test
    void reject_Success() {
        UUID adjId = UUID.randomUUID();
        StockAdjustment adj = new StockAdjustment();
        adj.setId(adjId);
        adj.setWarehouseId(warehouseId);
        adj.setRequestedBy(requesterId);
        adj.setReason(reason);
        adj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        when(adjustments.findWithLinesById(adjId)).thenReturn(Optional.of(adj));
        when(clock.instant()).thenReturn(Instant.now());
        when(adjustments.saveAndFlush(adj)).thenReturn(adj);

        AdjustmentResponse res = service.reject(adjId, approverId, "Not valid");

        assertNotNull(res);
        assertEquals(AdjustmentStatus.REJECTED, adj.getStatus());
        assertEquals("Not valid", adj.getRejectionReason());
    }

    @Test
    void reject_MissingReason_ThrowsBusinessException() {
        UUID adjId = UUID.randomUUID();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.reject(adjId, approverId, "   "));
        assertEquals("REJECTION_REASON_REQUIRED", ex.getCode());
    }

    @Test
    void cancel_Success() {
        UUID adjId = UUID.randomUUID();
        StockAdjustment adj = new StockAdjustment();
        adj.setId(adjId);
        adj.setWarehouseId(warehouseId);
        adj.setRequestedBy(requesterId);
        adj.setReason(reason);
        adj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        when(adjustments.findWithLinesById(adjId)).thenReturn(Optional.of(adj));
        when(clock.instant()).thenReturn(Instant.now());
        when(adjustments.saveAndFlush(adj)).thenReturn(adj);

        AdjustmentResponse res = service.cancel(adjId, requesterId);

        assertNotNull(res);
        assertEquals(AdjustmentStatus.CANCELLED, adj.getStatus());
    }

    @Test
    void cancel_ByOtherUser_ThrowsForbiddenException() {
        UUID adjId = UUID.randomUUID();
        StockAdjustment adj = new StockAdjustment();
        adj.setId(adjId);
        adj.setRequestedBy(requesterId);
        adj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        when(adjustments.findWithLinesById(adjId)).thenReturn(Optional.of(adj));

        assertThrows(ForbiddenException.class, () -> service.cancel(adjId, approverId));
    }

    @Test
    void accessInfo_Success() {
        UUID adjId = UUID.randomUUID();
        StockAdjustment adj = new StockAdjustment();
        adj.setWarehouseId(warehouseId);
        adj.setRequestedBy(requesterId);

        when(adjustments.findById(adjId)).thenReturn(Optional.of(adj));

        AdjustmentAccessInfo info = service.accessInfo(adjId);

        assertNotNull(info);
        assertEquals(warehouseId, info.warehouseId());
        assertEquals(requesterId, info.requestedBy());
    }

    @Test
    void search_Success() {
        AdjustmentFilter filter = new AdjustmentFilter(warehouseId, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        StockAdjustment adj = new StockAdjustment();
        adj.setId(UUID.randomUUID());
        adj.setWarehouseId(warehouseId);
        adj.setRequestedBy(requesterId);
        adj.setReason(reason);

        Page<StockAdjustment> page = new PageImpl<>(List.of(adj), pageable, 1);
        when(adjustments.search(eq(warehouseId), anySet(), any(), any(Pageable.class))).thenReturn(page);
        when(workers.findDisplayNames(anySet())).thenReturn(Map.of(requesterId, "User"));

        PageResponse<AdjustmentSummaryResponse> res = service.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }
}

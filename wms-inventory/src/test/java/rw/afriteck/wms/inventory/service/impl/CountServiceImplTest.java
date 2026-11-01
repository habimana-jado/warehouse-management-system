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
import rw.afriteck.wms.common.exception.ConflictException;
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
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockBalance;
import rw.afriteck.wms.inventory.entity.StockCount;
import rw.afriteck.wms.inventory.entity.StockCountLine;
import rw.afriteck.wms.inventory.enums.CountLineStatus;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.repository.LotRepository;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockCountLineRepository;
import rw.afriteck.wms.inventory.repository.StockCountRepository;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskLineStatus;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountServiceImplTest {

    @Mock
    private StockCountRepository counts;
    @Mock
    private StockCountLineRepository countLines;
    @Mock
    private StockBalanceRepository balances;
    @Mock
    private LotRepository lots;
    @Mock
    private InventoryService inventory;
    @Mock
    private TaskService taskService;
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
    private CountServiceImpl service;

    private UUID warehouseId;
    private UUID countId;
    private UUID itemId;
    private UUID locationId;
    private UUID actorId;
    private WarehouseResponse warehouse;
    private StockCount count;
    private ItemResponse item;
    private LocationResponse location;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        countId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();
        actorId = UUID.randomUUID();

        warehouse = new WarehouseResponse(warehouseId, "WH1", "Main Warehouse", "Addr", "City", "Country", true);
        count = new StockCount();
        count.setId(countId);
        count.setWarehouseId(warehouseId);
        count.setCountNumber("CNT-000001");
        count.setStatus(CountStatus.DRAFT);

        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.NONE, false, true);
        location = new LocationResponse(locationId, warehouseId, null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
    }

    @Test
    void create_Success() {
        CreateCountRequest req = new CreateCountRequest(warehouseId, null, "Cycle count");
        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(counts.nextNumber()).thenReturn(1L);
        when(counts.saveAndFlush(any(StockCount.class))).thenReturn(count);

        CountResponse res = service.create(req);

        assertNotNull(res);
        assertEquals("CNT-000001", res.countNumber());
    }

    @Test
    void create_MissingWarehouse_ThrowsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(new CreateCountRequest(null, null, null)));
        assertEquals("WAREHOUSE_REQUIRED", ex.getCode());
    }

    @Test
    void create_InactiveWarehouse_ThrowsBusinessException() {
        WarehouseResponse inactiveWH = new WarehouseResponse(warehouseId, "WH1", "Main WH", "Addr", "City", "Country", false);
        when(warehouseService.getById(warehouseId)).thenReturn(inactiveWH);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(new CreateCountRequest(warehouseId, null, null)));
        assertEquals("WAREHOUSE_INACTIVE", ex.getCode());
    }

    @Test
    void start_Success() {
        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(counts.findByWarehouseIdAndStatusIn(eq(warehouseId), anySet())).thenReturn(List.of());

        StockBalance balance = new StockBalance();
        balance.setLocationId(locationId);
        balance.setItemId(itemId);
        balance.setQuantity(BigDecimal.TEN);
        when(balances.findAllPositive(warehouseId)).thenReturn(List.of(balance));
        when(locationService.findAllByIds(anySet())).thenReturn(List.of(location));
        when(clock.instant()).thenReturn(Instant.now());

        UUID countLineId = UUID.randomUUID();
        TaskResponse taskRes = new TaskResponse(UUID.randomUUID(), 1L, "TASK-001", TaskType.COUNT, TaskStatus.OPEN, null, warehouseId, null,
                "CNT", countId, "CNT-000001", null, null, null, null, null, null, null, null, Instant.now(), List.of(
                        new TaskLineResponse(UUID.randomUUID(), 1, itemId, "SKU1", "Item 1", "LOT1", null, locationId, "BIN1", null, null, BigDecimal.TEN, BigDecimal.ZERO, TaskLineStatus.PENDING, countLineId, null)
        ));
        when(taskService.create(any())).thenReturn(taskRes);
        when(counts.saveAndFlush(any(StockCount.class))).thenReturn(count);

        CountResponse res = service.start(countId, new StartCountRequest(null, null));

        assertNotNull(res);
        assertEquals(CountStatus.IN_PROGRESS, count.getStatus());
        verify(countLines).saveAll(anyList());
    }

    @Test
    void start_NothingToCount_ThrowsBusinessException() {
        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(counts.findByWarehouseIdAndStatusIn(eq(warehouseId), anySet())).thenReturn(List.of());
        when(balances.findAllPositive(warehouseId)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.start(countId, null));
        assertEquals("COUNT_NOTHING_TO_COUNT", ex.getCode());
    }

    @Test
    void recordCount_Success() {
        count.setStatus(CountStatus.IN_PROGRESS);
        UUID lineId = UUID.randomUUID();
        StockCountLine line = new StockCountLine();
        line.setId(lineId);
        line.setCount(count);
        line.setItemId(itemId);
        line.setLocationId(locationId);
        line.setStatus(CountLineStatus.PENDING);

        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(countLines.findByIdAndCountId(lineId, countId)).thenReturn(Optional.of(line));
        when(balances.quantityUntracked(locationId, itemId)).thenReturn(Optional.of(BigDecimal.TEN));
        when(clock.instant()).thenReturn(Instant.now());
        when(countLines.saveAndFlush(line)).thenReturn(line);
        when(itemService.findAllByIds(anySet())).thenReturn(List.of(item));
        when(locationService.findAllByIds(anySet())).thenReturn(List.of(location));

        RecordCountRequest req = new RecordCountRequest(BigDecimal.valueOf(8));
        CountLineResponse res = service.recordCount(countId, lineId, req, actorId, true);

        assertNotNull(res);
        assertEquals(CountLineStatus.COUNTED, line.getStatus());
        assertEquals(BigDecimal.valueOf(8), line.getCountedQty());
    }

    @Test
    void addLine_Success() {
        count.setStatus(CountStatus.IN_PROGRESS);
        AddCountLineRequest req = new AddCountLineRequest(itemId, locationId, "LOT1", null, BigDecimal.valueOf(5));
        ItemResponse lotItem = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.LOT, false, true);

        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(itemService.getById(any())).thenReturn(lotItem);
        when(locationService.requireStorableBin(any())).thenReturn(location);

        Lot lot = new Lot();
        lot.setId(UUID.randomUUID());
        lot.setLotNumber("LOT1");
        when(lots.findByItemIdAndLotNumber(any(), eq("LOT1"))).thenReturn(Optional.of(lot));
        when(countLines.maxLineNo(countId)).thenReturn(1);
        when(clock.instant()).thenReturn(Instant.now());

        StockCountLine line = new StockCountLine();
        line.setId(UUID.randomUUID());
        line.setCount(count);
        line.setItemId(itemId);
        line.setLocationId(locationId);
        line.setLot(lot);
        line.setExpectedQty(BigDecimal.ZERO);
        line.setCountedQty(BigDecimal.valueOf(5));
        when(countLines.saveAndFlush(any(StockCountLine.class))).thenReturn(line);
        when(itemService.findAllByIds(anySet())).thenReturn(List.of(lotItem));
        when(locationService.findAllByIds(anySet())).thenReturn(List.of(location));

        CountLineResponse res = service.addLine(countId, req, actorId);

        assertNotNull(res);
        verify(countLines).saveAndFlush(any(StockCountLine.class));
    }

    @Test
    void submit_PendingLinesExist_ThrowsBusinessException() {
        count.setStatus(CountStatus.IN_PROGRESS);
        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(countLines.countByCountIdAndStatus(countId, CountLineStatus.PENDING)).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(countId));
        assertEquals("COUNT_LINES_PENDING", ex.getCode());
    }

    @Test
    void submit_Success() {
        count.setStatus(CountStatus.IN_PROGRESS);
        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(countLines.countByCountIdAndStatus(countId, CountLineStatus.PENDING)).thenReturn(0L);
        when(counts.saveAndFlush(count)).thenReturn(count);

        CountResponse res = service.submit(countId);

        assertNotNull(res);
        assertEquals(CountStatus.REVIEW, count.getStatus());
        verify(taskService).cancelByReference(any(), eq(countId), any());
    }

    @Test
    void approve_Success() {
        count.setStatus(CountStatus.REVIEW);
        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(countLines.findAllVariance(countId)).thenReturn(List.of());
        when(clock.instant()).thenReturn(Instant.now());
        when(counts.saveAndFlush(count)).thenReturn(count);

        CountResponse res = service.approve(countId, actorId);

        assertNotNull(res);
        assertEquals(CountStatus.COMPLETED, count.getStatus());
        assertEquals(actorId, count.getApprovedBy());
    }

    @Test
    void cancel_Success() {
        count.setStatus(CountStatus.IN_PROGRESS);
        when(counts.findById(countId)).thenReturn(Optional.of(count));
        when(clock.instant()).thenReturn(Instant.now());
        when(counts.saveAndFlush(count)).thenReturn(count);

        CountResponse res = service.cancel(countId, "Cancelled by user");

        assertNotNull(res);
        assertEquals(CountStatus.CANCELLED, count.getStatus());
        assertEquals("Cancelled by user", count.getCancelReason());
    }

    @Test
    void cancel_CompletedCount_ThrowsConflictException() {
        count.setStatus(CountStatus.COMPLETED);
        when(counts.findById(countId)).thenReturn(Optional.of(count));

        assertThrows(ConflictException.class, () -> service.cancel(countId, "Cancel"));
    }

    @Test
    void search_Success() {
        CountFilter filter = new CountFilter(warehouseId, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockCount> page = new PageImpl<>(List.of(count), pageable, 1);

        when(counts.search(eq(warehouseId), anySet(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<CountResponse> res = service.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }
}

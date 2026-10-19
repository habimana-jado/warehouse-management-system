package rw.afriteck.wms.outbound.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.*;
import rw.afriteck.wms.outbound.enums.PickLineStatus;
import rw.afriteck.wms.outbound.enums.PickListStatus;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.repository.PickLineRepository;
import rw.afriteck.wms.outbound.repository.PickListRepository;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.support.OutboundNumbers;
import rw.afriteck.wms.outbound.support.PickSuggester;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickListServiceImplTest {

    @Mock
    private SalesOrderRepository orders;
    @Mock
    private PickListRepository lists;
    @Mock
    private PickLineRepository pickLines;
    @Mock
    private TaskService taskService;
    @Mock
    private InventoryService inventory;
    @Mock
    private InventoryQueryService inventoryQuery;
    @Mock
    private ItemService items;
    @Mock
    private LocationService locations;
    @Mock
    private CustomerService customers;
    @Mock
    private WarehouseService warehouses;
    @Mock
    private PickSuggester suggester;
    @Mock
    private OutboundNumbers numbers;
    @Mock
    private Clock clock;

    @InjectMocks
    private PickListServiceImpl service;

    private UUID warehouseId;
    private UUID customerId;
    private UUID itemId;
    private UUID locationId;
    private CustomerResponse activeCustomer;
    private WarehouseResponse activeWarehouse;
    private ItemResponse activeItem;
    private LocationResponse activeLocation;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        activeCustomer = new CustomerResponse(customerId, "CUST001", "Acme", "Contact", "123", "c@t.com", "Addr", "Kigali", "Rwanda", true);
        activeWarehouse = new WarehouseResponse(warehouseId, "WH001", "Main WH", "Addr", "Kigali", "Rwanda", true);
        activeItem = new ItemResponse(itemId, "SKU001", "Item 1", "BAR001", null, "Category", "PCS", TrackingType.NONE, false, true);
        activeLocation = new LocationResponse(locationId, warehouseId, "A-01-01", "A-01-01", LocationType.SHELF, LocationUsage.STORAGE, true, true);

        fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void release_Success() {
        UUID salesOrderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(salesOrderId);
        so.setWarehouseId(warehouseId);
        so.setCustomerId(customerId);
        so.setStatus(SalesOrderStatus.CONFIRMED);

        SalesOrderLine sol = new SalesOrderLine();
        sol.setId(UUID.randomUUID());
        sol.setLineNo(1);
        sol.setItemId(itemId);
        sol.setOrderedQty(BigDecimal.TEN);
        sol.setReleasedQty(BigDecimal.ZERO);
        so.addLine(sol);

        when(orders.findByIdForUpdate(salesOrderId)).thenReturn(Optional.of(so));
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);
        when(suggester.suggest(eq(warehouseId), anyList()))
                .thenReturn(List.of(new PickSuggester.Suggestion(itemId, locationId, null, BigDecimal.TEN)));
        when(numbers.nextPickList()).thenReturn("PCK-000001");
        when(lists.saveAndFlush(any(PickList.class))).thenAnswer(i -> {
            PickList p = i.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            int lineNo = 1;
            for (PickLine pl : p.getLines()) {
                if (pl.getId() == null) pl.setId(UUID.randomUUID());
                pl.setLineNo(lineNo++);
            }
            return p;
        });

        UUID taskId = UUID.randomUUID();
        UUID taskLineId = UUID.randomUUID();
        when(taskService.create(any())).thenAnswer(i -> {
            CreateTaskRequest req = i.getArgument(0);
            UUID refLineId = req.lines().get(0).referenceLineId();
            return new TaskResponse(taskId, req.type(), TaskStatus.OPEN, req.priority(), warehouseId, null, null,
                    req.referenceType(), req.referenceId(), req.referenceNumber(), req.dueAt(), req.notes(),
                    List.of(new TaskLineResponse(taskLineId, itemId, null, locationId, null, BigDecimal.TEN, BigDecimal.ZERO, refLineId)),
                    fixedInstant, null, null);
        });
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(locations.findAllByIds(Set.of(locationId))).thenReturn(List.of(activeLocation));

        PickListResponse response = service.release(salesOrderId, new ReleaseSalesOrderRequest(TaskPriority.MEDIUM, null, "Notes"));

        assertNotNull(response);
        assertEquals("PCK-000001", response.pickNumber());
        assertEquals(PickListStatus.RELEASED, response.status());
        verify(lists, times(2)).saveAndFlush(any(PickList.class));
    }

    @Test
    void confirmPick_Success() {
        UUID pickListId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        SalesOrder so = new SalesOrder();
        so.setId(salesOrderId);
        so.setWarehouseId(warehouseId);
        so.setCustomerId(customerId);

        SalesOrderLine sol = new SalesOrderLine();
        sol.setId(UUID.randomUUID());
        sol.setItemId(itemId);
        sol.setOrderedQty(BigDecimal.TEN);
        sol.setReleasedQty(BigDecimal.TEN);
        sol.setPickedQty(BigDecimal.ZERO);
        so.addLine(sol);

        PickList list = new PickList();
        list.setId(pickListId);
        list.setPickNumber("PCK-000001");
        list.setSalesOrder(so);
        list.setWarehouseId(warehouseId);
        list.setStatus(PickListStatus.RELEASED);
        list.setTaskId(taskId);

        PickLine pl = new PickLine();
        pl.setId(lineId);
        pl.setPickList(list);
        pl.setSalesOrderLine(sol);
        pl.setItemId(itemId);
        pl.setPlannedQty(BigDecimal.TEN);
        pl.setPickedQty(BigDecimal.ZERO);
        pl.setStatus(PickLineStatus.PENDING);
        pl.setTaskLineId(UUID.randomUUID());
        list.addLine(pl);

        when(pickLines.findSalesOrderIdById(lineId)).thenReturn(Optional.of(salesOrderId));
        when(orders.findByIdForUpdate(salesOrderId)).thenReturn(Optional.of(so));
        when(pickLines.findById(lineId)).thenReturn(Optional.of(pl));
        when(taskService.getById(taskId)).thenReturn(new TaskResponse(taskId, TaskType.PICK, TaskStatus.ASSIGNED, TaskPriority.MEDIUM,
                warehouseId, actorId, "Worker", "PICK_LIST", pickListId, "PCK-000001", null, null, List.of(), fixedInstant, null, null));
        when(items.getById(itemId)).thenReturn(activeItem);
        when(locations.findAllByIds(Set.of(locationId))).thenReturn(List.of(activeLocation));
        when(lists.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));

        ConfirmPickRequest req = new ConfirmPickRequest(List.of(new PickAllocationRequest(locationId, null, null, BigDecimal.TEN)), null, false);
        PickListResponse response = service.confirmPick(pickListId, lineId, req, actorId, false);

        assertNotNull(response);
        assertEquals(PickListStatus.PICKED, response.status());
        verify(inventory).post(any(PostStockRequest.class));
        verify(taskService).completeLine(eq(taskId), eq(pl.getTaskLineId()), eq(BigDecimal.TEN), eq(null));
    }

    @Test
    void confirmPick_WrongAssignee_ThrowsForbiddenException() {
        UUID pickListId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID assignedWorkerId = UUID.randomUUID();

        SalesOrder so = new SalesOrder();
        so.setId(salesOrderId);

        PickList list = new PickList();
        list.setId(pickListId);
        list.setSalesOrder(so);
        list.setStatus(PickListStatus.RELEASED);
        list.setTaskId(taskId);

        PickLine pl = new PickLine();
        pl.setId(lineId);
        pl.setPickList(list);
        pl.setStatus(PickLineStatus.PENDING);
        list.addLine(pl);

        when(pickLines.findSalesOrderIdById(lineId)).thenReturn(Optional.of(salesOrderId));
        when(orders.findByIdForUpdate(salesOrderId)).thenReturn(Optional.of(so));
        when(pickLines.findById(lineId)).thenReturn(Optional.of(pl));
        when(taskService.getById(taskId)).thenReturn(new TaskResponse(taskId, TaskType.PICK, TaskStatus.ASSIGNED, TaskPriority.MEDIUM,
                warehouseId, assignedWorkerId, "Worker", "PICK_LIST", pickListId, "PCK-000001", null, null, List.of(), fixedInstant, null, null));

        ConfirmPickRequest req = new ConfirmPickRequest(List.of(), null, false);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.confirmPick(pickListId, lineId, req, actorId, false));
        assertEquals("This pick belongs to another user", ex.getMessage());
    }

    @Test
    void cancel_Success() {
        UUID pickListId = UUID.randomUUID();
        UUID salesOrderId = UUID.randomUUID();

        SalesOrder so = new SalesOrder();
        so.setId(salesOrderId);
        so.setCustomerId(customerId);

        PickList list = new PickList();
        list.setId(pickListId);
        list.setSalesOrder(so);
        list.setStatus(PickListStatus.RELEASED);

        PickLine pl = new PickLine();
        pl.setId(UUID.randomUUID());
        pl.setItemId(itemId);
        pl.setPlannedQty(BigDecimal.TEN);
        pl.setPickedQty(BigDecimal.ZERO);
        pl.setStatus(PickLineStatus.PENDING);

        SalesOrderLine sol = new SalesOrderLine();
        sol.setReleasedQty(BigDecimal.TEN);
        pl.setSalesOrderLine(sol);
        list.addLine(pl);

        when(lists.findById(pickListId)).thenReturn(list);
        when(orders.findByIdForUpdate(salesOrderId)).thenReturn(Optional.of(so));
        when(lists.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(customers.getById(customerId)).thenReturn(activeCustomer);

        PickListResponse response = service.cancel(pickListId, new CancelPickListRequest("Manager cancelled"));

        assertNotNull(response);
        assertEquals(PickListStatus.CANCELLED, response.status());
        assertEquals("Manager cancelled", response.cancelReason());
        assertEquals(BigDecimal.ZERO, sol.getReleasedQty());
    }

    @Test
    void search_Success() {
        PickList list = new PickList();
        list.setId(UUID.randomUUID());
        list.setWarehouseId(warehouseId);
        list.setStatus(PickListStatus.RELEASED);
        SalesOrder so = new SalesOrder();
        so.setCustomerId(customerId);
        list.setSalesOrder(so);

        when(lists.search(eq(warehouseId), anySet(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(list)));

        PageResponse<PickListSummaryResponse> res = service.search(warehouseId, null, PageRequest.of(0, 10));

        assertNotNull(res);
        assertEquals(1, res.content().size());
    }
}

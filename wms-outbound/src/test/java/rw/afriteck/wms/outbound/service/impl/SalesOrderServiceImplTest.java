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
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.support.OutboundNumbers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesOrderServiceImplTest {

    @Mock
    private SalesOrderRepository orders;
    @Mock
    private PickListService pickLists;
    @Mock
    private CustomerService customers;
    @Mock
    private WarehouseService warehouses;
    @Mock
    private ItemService items;
    @Mock
    private InventoryQueryService inventoryQuery;
    @Mock
    private OutboundNumbers numbers;
    @Mock
    private Clock clock;

    @InjectMocks
    private SalesOrderServiceImpl service;

    private UUID customerId;
    private UUID warehouseId;
    private UUID itemId;
    private CustomerResponse activeCustomer;
    private WarehouseResponse activeWarehouse;
    private ItemResponse activeItem;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        activeCustomer = new CustomerResponse(customerId, "CUST001", "Acme Corp", "Contact", "123", "cust@test.com", "Addr", "Kigali", "Rwanda", true);
        activeWarehouse = new WarehouseResponse(warehouseId, "WH001", "Main WH", "Addr", "Kigali", "Rwanda", true);
        activeItem = new ItemResponse(itemId, "SKU001", "Item 1", "BAR001", null, "Category", "PCS", TrackingType.NONE, false, true);

        fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void create_Success() {
        CreateSalesOrderRequest req = new CreateSalesOrderRequest(
                customerId, warehouseId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10),
                "USD", "PO-123", "Order notes",
                List.of(new SalesOrderLineRequest(itemId, BigDecimal.TEN, "PCS", BigDecimal.valueOf(50), "Line note")));

        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);
        when(numbers.nextSalesOrder()).thenReturn("SO-000001");
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(items.toBaseQuantity(eq(itemId), eq("PCS"), eq(BigDecimal.TEN))).thenReturn(BigDecimal.TEN);
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(invocation -> {
            SalesOrder so = invocation.getArgument(0);
            so.setId(UUID.randomUUID());
            return so;
        });

        SalesOrderResponse response = service.create(req);

        assertNotNull(response);
        assertEquals("SO-000001", response.orderNumber());
        assertEquals(SalesOrderStatus.DRAFT, response.status());
        assertEquals(1, response.lines().size());
        verify(orders).saveAndFlush(any(SalesOrder.class));
    }

    @Test
    void create_MissingCustomerOrWarehouse_ThrowsException() {
        CreateSalesOrderRequest req = new CreateSalesOrderRequest(
                null, warehouseId, LocalDate.now(), null, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("SO_FIELDS_REQUIRED", ex.getCode());
    }

    @Test
    void create_InactiveCustomer_ThrowsException() {
        CustomerResponse inactiveCustomer = new CustomerResponse(customerId, "CUST001", "Acme", null, null, null, null, null, null, false);
        when(customers.getById(customerId)).thenReturn(inactiveCustomer);

        CreateSalesOrderRequest req = new CreateSalesOrderRequest(
                customerId, warehouseId, null, null, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("CUSTOMER_INACTIVE", ex.getCode());
    }

    @Test
    void update_Success() {
        UUID orderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(orderId);
        so.setStatus(SalesOrderStatus.DRAFT);
        so.setCustomerId(customerId);
        so.setWarehouseId(warehouseId);
        so.setOrderDate(LocalDate.of(2026, 1, 1));

        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(so));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(items.findAllByIds(anySet())).thenReturn(List.of(activeItem));
        when(items.toBaseQuantity(eq(itemId), eq("PCS"), eq(BigDecimal.valueOf(5)))).thenReturn(BigDecimal.valueOf(5));
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(i -> i.getArgument(0));

        UpdateSalesOrderRequest req = new UpdateSalesOrderRequest(
                LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 15), "USD", "REF-UPDATED", "Updated notes",
                List.of(new SalesOrderLineRequest(itemId, BigDecimal.valueOf(5), "PCS", BigDecimal.valueOf(45), null)));

        SalesOrderResponse response = service.update(orderId, req);

        assertNotNull(response);
        assertEquals("REF-UPDATED", response.customerReference());
        verify(orders).saveAndFlush(so);
    }

    @Test
    void confirm_Success() {
        UUID orderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(orderId);
        so.setStatus(SalesOrderStatus.DRAFT);
        so.setCustomerId(customerId);
        so.setWarehouseId(warehouseId);

        SalesOrderLine line = new SalesOrderLine();
        line.setId(UUID.randomUUID());
        line.setItemId(itemId);
        line.setOrderedQty(BigDecimal.TEN);
        so.addLine(line);

        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(so));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(inventoryQuery.getAvailableInStorage(warehouseId, Set.of(itemId)))
                .thenReturn(Map.of(itemId, BigDecimal.valueOf(5)));
        when(orders.saveAndFlush(any(SalesOrder.class))).thenAnswer(i -> i.getArgument(0));

        ConfirmSalesOrderResponse response = service.confirm(orderId);

        assertEquals(SalesOrderStatus.CONFIRMED, response.order().status());
        assertEquals(1, response.warnings().size());
        verify(orders).saveAndFlush(so);
    }

    @Test
    void cancel_Success() {
        UUID orderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(orderId);
        so.setStatus(SalesOrderStatus.CONFIRMED);
        so.setCustomerId(customerId);

        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(so));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(orders.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SalesOrderResponse response = service.cancel(orderId, new CancelSalesOrderRequest("Customer request"));

        assertEquals(SalesOrderStatus.CANCELLED, response.status());
        verify(pickLists).cancelOpenForOrder(orderId, "Customer request");
    }

    @Test
    void cancel_WithoutReason_ThrowsException() {
        UUID orderId = UUID.randomUUID();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancel(orderId, null));
        assertEquals("CANCEL_REASON_REQUIRED", ex.getCode());
    }

    @Test
    void close_Success() {
        UUID orderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(orderId);
        so.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);
        so.setCustomerId(customerId);

        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(so));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(orders.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SalesOrderResponse response = service.close(orderId);

        assertEquals(SalesOrderStatus.CLOSED, response.status());
        verify(pickLists).cancelOpenForOrder(orderId, "Order closed short");
    }

    @Test
    void close_WithUnshippedPicks_ThrowsConflictException() {
        UUID orderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(orderId);
        so.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);

        SalesOrderLine line = new SalesOrderLine();
        line.setPickedQty(BigDecimal.TEN);
        line.setShippedQty(BigDecimal.valueOf(5));
        so.addLine(line);

        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(so));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.close(orderId));
        assertEquals("SO_HAS_UNSHIPPED_PICKS", ex.getCode());
    }

    @Test
    void search_Success() {
        SalesOrderFilter filter = new SalesOrderFilter(
                warehouseId, null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        SalesOrder so = new SalesOrder();
        so.setId(UUID.randomUUID());
        so.setCustomerId(customerId);
        so.setWarehouseId(warehouseId);
        so.setOrderDate(LocalDate.now());
        so.setStatus(SalesOrderStatus.DRAFT);

        when(orders.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(so)));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(orders.totalsByOrder(List.of(so.getId()))).thenReturn(List.of());

        PageResponse<SalesOrderSummaryResponse> res = service.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.content().size());
    }
}

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
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;
import rw.afriteck.wms.outbound.entity.Shipment;
import rw.afriteck.wms.outbound.entity.ShipmentLine;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.enums.ShipmentStatus;
import rw.afriteck.wms.outbound.repository.SalesOrderRepository;
import rw.afriteck.wms.outbound.repository.ShipmentRepository;
import rw.afriteck.wms.outbound.support.OutboundNumbers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceImplTest {

    @Mock
    private SalesOrderRepository orders;
    @Mock
    private ShipmentRepository shipments;
    @Mock
    private CustomerService customers;
    @Mock
    private ItemService items;
    @Mock
    private OutboundNumbers numbers;
    @Mock
    private Clock clock;

    @InjectMocks
    private ShipmentServiceImpl service;

    private UUID warehouseId;
    private UUID customerId;
    private UUID itemId;
    private CustomerResponse activeCustomer;
    private ItemResponse activeItem;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        activeCustomer = new CustomerResponse(customerId, "CUST001", "Acme", "Contact", "123", "c@t.com", "Addr", "Kigali", "Rwanda", true);
        activeItem = new ItemResponse(itemId, "SKU001", "Item 1", "BAR001", null, "Category", "PCS", TrackingType.NONE, false, true);

        fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void create_Success() {
        UUID salesOrderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(salesOrderId);
        so.setWarehouseId(warehouseId);
        so.setCustomerId(customerId);
        so.setStatus(SalesOrderStatus.CONFIRMED);

        SalesOrderLine sol = new SalesOrderLine();
        sol.setId(UUID.randomUUID());
        sol.setItemId(itemId);
        sol.setOrderedQty(BigDecimal.TEN);
        sol.setPickedQty(BigDecimal.TEN);
        sol.setShippedQty(BigDecimal.ZERO);
        so.addLine(sol);

        when(orders.findByIdForUpdate(salesOrderId)).thenReturn(Optional.of(so));
        when(numbers.nextShipment()).thenReturn("DN-000001");
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(shipments.saveAndFlush(any(Shipment.class))).thenAnswer(i -> {
            Shipment s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        CreateShipmentRequest req = new CreateShipmentRequest("Custom Address", "DHL", "TRACK123", 2, "Shipment note", List.of());
        ShipmentResponse response = service.create(salesOrderId, req);

        assertNotNull(response);
        assertEquals("DN-000001", response.shipmentNumber());
        assertEquals(ShipmentStatus.DISPATCHED, response.status());
        assertEquals(SalesOrderStatus.SHIPPED, so.getStatus());
        verify(shipments).saveAndFlush(any(Shipment.class));
    }

    @Test
    void create_NothingToShip_ThrowsException() {
        UUID salesOrderId = UUID.randomUUID();
        SalesOrder so = new SalesOrder();
        so.setId(salesOrderId);
        so.setStatus(SalesOrderStatus.CONFIRMED);

        SalesOrderLine sol = new SalesOrderLine();
        sol.setItemId(itemId);
        sol.setOrderedQty(BigDecimal.TEN);
        sol.setPickedQty(BigDecimal.ZERO);
        sol.setShippedQty(BigDecimal.ZERO);
        so.addLine(sol);

        when(orders.findByIdForUpdate(salesOrderId)).thenReturn(Optional.of(so));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(salesOrderId, null));
        assertEquals("SHIPMENT_NOTHING_TO_SHIP", ex.getCode());
    }

    @Test
    void confirmDelivery_Success() {
        UUID shipmentId = UUID.randomUUID();
        Shipment s = new Shipment();
        s.setId(shipmentId);
        s.setStatus(ShipmentStatus.DISPATCHED);
        s.setShippedAt(fixedInstant.minusSeconds(3600));
        s.setCustomerId(customerId);

        when(shipments.findById(shipmentId)).thenReturn(Optional.of(s));
        when(customers.getById(customerId)).thenReturn(activeCustomer);
        when(items.findAllByIds(anySet())).thenReturn(List.of());
        when(shipments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ConfirmDeliveryRequest req = new ConfirmDeliveryRequest(fixedInstant, "John Doe", "All good");
        ShipmentResponse response = service.confirmDelivery(shipmentId, req);

        assertNotNull(response);
        assertEquals(ShipmentStatus.DELIVERED, response.status());
        assertEquals("John Doe", response.receivedByName());
    }

    @Test
    void confirmDelivery_InvalidState_ThrowsConflictException() {
        UUID shipmentId = UUID.randomUUID();
        Shipment s = new Shipment();
        s.setId(shipmentId);
        s.setStatus(ShipmentStatus.DELIVERED);

        when(shipments.findById(shipmentId)).thenReturn(Optional.of(s));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.confirmDelivery(shipmentId, null));
        assertEquals("SHIPMENT_INVALID_STATE", ex.getCode());
    }

    @Test
    void search_Success() {
        Shipment s = new Shipment();
        s.setId(UUID.randomUUID());
        s.setWarehouseId(warehouseId);
        s.setCustomerId(customerId);
        s.setStatus(ShipmentStatus.DISPATCHED);

        when(shipments.search(eq(warehouseId), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(s)));
        when(customers.getById(customerId)).thenReturn(activeCustomer);

        PageResponse<ShipmentSummaryResponse> res = service.search(warehouseId, "DN", PageRequest.of(0, 10));

        assertNotNull(res);
        assertEquals(1, res.content().size());
    }
}

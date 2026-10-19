package rw.afriteck.wms.inbound.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.security.WorkerLookup;
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
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.entity.CustomerReturn;
import rw.afriteck.wms.inbound.entity.CustomerReturnLine;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.enums.ReturnDisposition;
import rw.afriteck.wms.inbound.repository.CustomerReturnLineRepository;
import rw.afriteck.wms.inbound.repository.CustomerReturnRepository;
import rw.afriteck.wms.inbound.support.PutawaySuggester;
import rw.afriteck.wms.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerReturnServiceImplTest {

    @Mock
    private CustomerReturnRepository returns;
    @Mock
    private CustomerReturnLineRepository returnLines;
    @Mock
    private WarehouseService warehouses;
    @Mock
    private CustomerService customers;
    @Mock
    private LocationService locations;
    @Mock
    private ItemService items;
    @Mock
    private InventoryService inventory;
    @Mock
    private PutawaySuggester suggester;
    @Mock
    private WorkerLookup workers;
    @Mock
    private Clock clock;

    @InjectMocks
    private CustomerReturnServiceImpl service;

    private UUID warehouseId;
    private UUID customerId;
    private UUID itemId;
    private UUID storageLocationId;

    private WarehouseResponse warehouse;
    private CustomerResponse customer;
    private ItemResponse item;
    private LocationResponse storageLocation;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        storageLocationId = UUID.randomUUID();

        warehouse = new WarehouseResponse(warehouseId, "WH1", "Warehouse 1", "Addr", "Kigali", "Rwanda", true);
        customer = new CustomerResponse(customerId, "CUST001", "Customer 1", "Contact", "123", "cust@test.com", "Addr", "Kigali", "Rwanda", true);
        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Cat", "PCS", TrackingType.NONE, false, true);
        storageLocation = new LocationResponse(storageLocationId, warehouseId, null, "Storage Bin", "STO01", LocationType.BIN, LocationUsage.STORAGE, null, null, true);

        lenient().when(clock.instant()).thenReturn(Instant.now());
    }

    @Test
    void create_Success() {
        CreateCustomerReturnRequest req = new CreateCustomerReturnRequest(
                warehouseId, customerId, UUID.randomUUID(), "SO-100", "Notes",
                List.of(new CustomerReturnLineRequest(itemId, BigDecimal.valueOf(5), "PCS", null, null, null, "Defective")));

        when(warehouses.getById(warehouseId)).thenReturn(warehouse);
        when(customers.getById(customerId)).thenReturn(customer);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(returns.nextReturnNumber()).thenReturn(1L);
        when(returns.saveAndFlush(any(CustomerReturn.class))).thenAnswer(i -> {
            CustomerReturn r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });

        CustomerReturnResponse response = service.create(req);

        assertNotNull(response);
        assertEquals("RMA-000001", response.returnNumber());
        assertEquals(CustomerReturnStatus.EXPECTED, response.status());
        assertEquals(1, response.lines().size());
        verify(returns).saveAndFlush(any());
    }

    @Test
    void create_NoLines_ThrowsException() {
        CreateCustomerReturnRequest req = new CreateCustomerReturnRequest(
                warehouseId, customerId, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("RETURN_NO_LINES", ex.getCode());
    }

    @Test
    void receive_Success() {
        UUID returnId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CustomerReturn ret = new CustomerReturn();
        ret.setId(returnId);
        ret.setWarehouseId(warehouseId);
        ret.setStatus(CustomerReturnStatus.EXPECTED);

        CustomerReturnLine line = new CustomerReturnLine();
        line.setId(lineId);
        line.setItemId(itemId);
        line.setExpectedQty(BigDecimal.valueOf(5));
        ret.addLine(line);

        when(returns.findByIdForUpdate(returnId)).thenReturn(Optional.of(ret));
        when(warehouses.getById(warehouseId)).thenReturn(warehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locations.requireStorableBin(storageLocationId)).thenReturn(storageLocation);
        when(returns.saveAndFlush(any(CustomerReturn.class))).thenAnswer(i -> i.getArgument(0));

        ReceiveReturnLineRequest lineReq = new ReceiveReturnLineRequest(
                lineId, "PCS", BigDecimal.valueOf(5), ReturnDisposition.RESTOCK, storageLocationId, null, null, null);
        ReceiveCustomerReturnRequest req = new ReceiveCustomerReturnRequest("Receive notes", List.of(lineReq));

        CustomerReturnResponse response = service.receive(returnId, req, actorId);

        assertNotNull(response);
        assertEquals(CustomerReturnStatus.RECEIVED, response.status());
        verify(inventory).post(any());
    }

    @Test
    void receive_QtyExceedsExpected_ThrowsException() {
        UUID returnId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        CustomerReturn ret = new CustomerReturn();
        ret.setId(returnId);
        ret.setWarehouseId(warehouseId);
        ret.setStatus(CustomerReturnStatus.EXPECTED);

        CustomerReturnLine line = new CustomerReturnLine();
        line.setId(lineId);
        line.setItemId(itemId);
        line.setExpectedQty(BigDecimal.valueOf(5));
        ret.addLine(line);

        when(returns.findByIdForUpdate(returnId)).thenReturn(Optional.of(ret));
        when(warehouses.getById(warehouseId)).thenReturn(warehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));

        ReceiveReturnLineRequest lineReq = new ReceiveReturnLineRequest(
                lineId, "PCS", BigDecimal.valueOf(10), ReturnDisposition.RESTOCK, storageLocationId, null, null, null);
        ReceiveCustomerReturnRequest req = new ReceiveCustomerReturnRequest(null, List.of(lineReq));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(returnId, req, actorId));
        assertEquals("RETURN_QTY_EXCEEDS_EXPECTED", ex.getCode());
    }

    @Test
    void cancel_Success() {
        UUID returnId = UUID.randomUUID();

        CustomerReturn ret = new CustomerReturn();
        ret.setId(returnId);
        ret.setWarehouseId(warehouseId);
        ret.setStatus(CustomerReturnStatus.EXPECTED);

        when(returns.findByIdForUpdate(returnId)).thenReturn(Optional.of(ret));
        when(returns.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        CustomerReturnResponse response = service.cancel(returnId, "Wrong order");

        assertNotNull(response);
        assertEquals(CustomerReturnStatus.CANCELLED, response.status());
    }

    @Test
    void cancel_AlreadyReceived_ThrowsConflictException() {
        UUID returnId = UUID.randomUUID();

        CustomerReturn ret = new CustomerReturn();
        ret.setId(returnId);
        ret.setStatus(CustomerReturnStatus.RECEIVED);

        when(returns.findByIdForUpdate(returnId)).thenReturn(Optional.of(ret));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.cancel(returnId, "Reason"));
        assertEquals("RETURN_INVALID_STATE", ex.getCode());
    }
}

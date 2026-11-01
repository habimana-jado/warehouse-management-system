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
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.SupplierService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.SupplierReturn;
import rw.afriteck.wms.outbound.entity.SupplierReturnLine;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import rw.afriteck.wms.outbound.repository.SupplierReturnRepository;
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
class SupplierReturnServiceImplTest {

    @Mock
    private SupplierReturnRepository returns;
    @Mock
    private SupplierService suppliers;
    @Mock
    private WarehouseService warehouses;
    @Mock
    private ItemService items;
    @Mock
    private LocationService locations;
    @Mock
    private InventoryService inventory;
    @Mock
    private OutboundNumbers numbers;
    @Mock
    private Clock clock;

    @InjectMocks
    private SupplierReturnServiceImpl service;

    private UUID supplierId;
    private UUID warehouseId;
    private UUID itemId;
    private UUID locationId;
    private SupplierResponse activeSupplier;
    private WarehouseResponse activeWarehouse;
    private ItemResponse activeItem;
    private LocationResponse activeLocation;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        supplierId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        activeSupplier = new SupplierResponse(supplierId, "SUP001", "Supplier 1", "Contact", "123", "sup@test.com", "Addr", "Kigali", "Rwanda", true);
        activeWarehouse = new WarehouseResponse(warehouseId, "WH001", "Main WH", "Addr", "Kigali", "Rwanda", true);
        activeItem = new ItemResponse(itemId, "SKU001", "Item 1", "BAR001", null, "Category", "PCS", TrackingType.NONE, false, true);
        activeLocation = new LocationResponse(locationId, warehouseId, null, "Location 1", "BIN-01", LocationType.BIN, LocationUsage.STORAGE, "BAR1", BigDecimal.valueOf(100), true);

        fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void create_Success() {
        CreateSupplierReturnRequest req = new CreateSupplierReturnRequest(
                supplierId, warehouseId, LocalDate.of(2026, 1, 1), "Defective goods", "REF-SUP-123", "Some notes",
                List.of(new SupplierReturnLineRequest(itemId, locationId, null, null, BigDecimal.TEN, "PCS", "Line note")));

        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);
        when(numbers.nextReturn()).thenReturn("RTV-000001");
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(items.toBaseQuantity(eq(itemId), eq("PCS"), eq(BigDecimal.TEN))).thenReturn(BigDecimal.TEN);
        when(locations.findAllByIds(Set.of(locationId))).thenReturn(List.of(activeLocation));
        when(returns.saveAndFlush(any(SupplierReturn.class))).thenAnswer(i -> {
            SupplierReturn rtv = i.getArgument(0);
            rtv.setId(UUID.randomUUID());
            return rtv;
        });

        SupplierReturnResponse response = service.create(req);

        assertNotNull(response);
        assertEquals("RTV-000001", response.returnNumber());
        assertEquals(SupplierReturnStatus.DRAFT, response.status());
        verify(inventory).check(any(PostStockRequest.class));
        verify(returns).saveAndFlush(any(SupplierReturn.class));
    }

    @Test
    void create_MissingRequiredFields_ThrowsException() {
        CreateSupplierReturnRequest req = new CreateSupplierReturnRequest(
                null, warehouseId, null, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("RTV_FIELDS_REQUIRED", ex.getCode());
    }

    @Test
    void post_Success() {
        UUID rtvId = UUID.randomUUID();
        SupplierReturn rtv = new SupplierReturn();
        rtv.setId(rtvId);
        rtv.setReturnNumber("RTV-000001");
        rtv.setStatus(SupplierReturnStatus.DRAFT);
        rtv.setSupplierId(supplierId);
        rtv.setWarehouseId(warehouseId);

        SupplierReturnLine line = new SupplierReturnLine();
        line.setId(UUID.randomUUID());
        line.setItemId(itemId);
        line.setLocationId(locationId);
        line.setQuantity(BigDecimal.TEN);
        rtv.addLine(line);

        when(returns.findByIdForUpdate(rtvId)).thenReturn(Optional.of(rtv));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(locations.findAllByIds(Set.of(locationId))).thenReturn(List.of(activeLocation));
        when(returns.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SupplierReturnResponse response = service.post(rtvId);

        assertNotNull(response);
        assertEquals(SupplierReturnStatus.POSTED, response.status());
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void post_NotInDraft_ThrowsConflictException() {
        UUID rtvId = UUID.randomUUID();
        SupplierReturn rtv = new SupplierReturn();
        rtv.setId(rtvId);
        rtv.setStatus(SupplierReturnStatus.POSTED);

        when(returns.findByIdForUpdate(rtvId)).thenReturn(Optional.of(rtv));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.post(rtvId));
        assertEquals("RTV_INVALID_STATE", ex.getCode());
    }

    @Test
    void cancel_Success() {
        UUID rtvId = UUID.randomUUID();
        SupplierReturn rtv = new SupplierReturn();
        rtv.setId(rtvId);
        rtv.setStatus(SupplierReturnStatus.DRAFT);
        rtv.setSupplierId(supplierId);

        when(returns.findByIdForUpdate(rtvId)).thenReturn(Optional.of(rtv));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(returns.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SupplierReturnResponse response = service.cancel(rtvId, new CancelSupplierReturnRequest("Cancelled by user"));

        assertNotNull(response);
        assertEquals(SupplierReturnStatus.CANCELLED, response.status());
        assertEquals("Cancelled by user", response.cancelReason());
    }

    @Test
    void search_Success() {
        SupplierReturn rtv = new SupplierReturn();
        rtv.setId(UUID.randomUUID());
        rtv.setWarehouseId(warehouseId);
        rtv.setSupplierId(supplierId);
        rtv.setStatus(SupplierReturnStatus.DRAFT);

        when(returns.search(eq(warehouseId), anySet(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rtv)));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);

        PageResponse<SupplierReturnSummaryResponse> res = service.search(warehouseId, null, "RTV", PageRequest.of(0, 10));

        assertNotNull(res);
        assertEquals(1, res.content().size());
    }
}

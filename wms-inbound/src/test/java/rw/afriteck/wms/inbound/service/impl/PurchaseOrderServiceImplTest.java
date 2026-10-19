package rw.afriteck.wms.inbound.service.impl;

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
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.foundation.dtos.BarcodeResolution;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.SupplierService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.repository.PurchaseOrderLineRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;

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
class PurchaseOrderServiceImplTest {

    @Mock
    private PurchaseOrderRepository purchaseOrders;
    @Mock
    private PurchaseOrderLineRepository orderLines;
    @Mock
    private SupplierService suppliers;
    @Mock
    private WarehouseService warehouses;
    @Mock
    private ItemService items;
    @Mock
    private WorkerLookup workers;
    @Mock
    private Clock clock;

    @InjectMocks
    private PurchaseOrderServiceImpl service;

    private UUID supplierId;
    private UUID warehouseId;
    private UUID itemId;
    private SupplierResponse activeSupplier;
    private WarehouseResponse activeWarehouse;
    private ItemResponse activeItem;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        supplierId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        activeSupplier = new SupplierResponse(supplierId, "SUP001", "Supplier 1", "Contact", "123", "sup@test.com", "Addr", "Kigali", "Rwanda", true);
        activeWarehouse = new WarehouseResponse(warehouseId, "WH001", "Main WH", "Addr", "Kigali", "Rwanda", true);
        activeItem = new ItemResponse(itemId, "SKU001", "Item 1", "BAR001", null, "Category", "PCS", TrackingType.NONE, false, true);

        fixedInstant = Instant.parse("2026-01-01T10:00:00Z");
        lenient().when(clock.instant()).thenReturn(fixedInstant);
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void create_Success() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest(
                supplierId, warehouseId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10),
                "REF123", "USD", "Some notes",
                List.of(new PurchaseOrderLineRequest(null, itemId, BigDecimal.TEN, "PCS", BigDecimal.valueOf(100), BigDecimal.ZERO, "Line note")));

        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);
        when(purchaseOrders.nextPoNumber()).thenReturn(1L);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(purchaseOrders.saveAndFlush(any(PurchaseOrder.class))).thenAnswer(invocation -> {
            PurchaseOrder po = invocation.getArgument(0);
            po.setId(UUID.randomUUID());
            return po;
        });

        PurchaseOrderResponse response = service.create(req);

        assertNotNull(response);
        assertEquals("PO-000001", response.poNumber());
        assertEquals(PurchaseOrderStatus.DRAFT, response.status());
        assertEquals(1, response.lines().size());
        verify(purchaseOrders).saveAndFlush(any(PurchaseOrder.class));
    }

    @Test
    void create_MissingSupplierOrWarehouse_ThrowsException() {
        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest(
                null, warehouseId, LocalDate.now(), null, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("PO_FIELDS_REQUIRED", ex.getCode());
    }

    @Test
    void create_InactiveSupplier_ThrowsException() {
        SupplierResponse inactiveSupplier = new SupplierResponse(supplierId, "SUP001", "Supplier 1", null, null, null, null, null, null, false);
        when(suppliers.getById(supplierId)).thenReturn(inactiveSupplier);

        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest(
                supplierId, warehouseId, null, null, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("SUPPLIER_INACTIVE", ex.getCode());
    }

    @Test
    void create_ExpectedDateBeforeOrderDate_ThrowsException() {
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);

        CreatePurchaseOrderRequest req = new CreatePurchaseOrderRequest(
                supplierId, warehouseId, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1),
                null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("PO_DATES_INVALID", ex.getCode());
    }

    @Test
    void update_Success() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setSupplierId(supplierId);
        po.setWarehouseId(warehouseId);
        po.setOrderDate(LocalDate.of(2026, 1, 1));

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(items.findAllByIds(anySet())).thenReturn(List.of(activeItem));
        when(purchaseOrders.saveAndFlush(any(PurchaseOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePurchaseOrderRequest req = new UpdatePurchaseOrderRequest(
                LocalDate.of(2026, 1, 15), "REF-UPDATED", "USD", "Updated notes",
                List.of(new PurchaseOrderLineRequest(null, itemId, BigDecimal.valueOf(5), "PCS", BigDecimal.valueOf(50), BigDecimal.ZERO, null)));

        PurchaseOrderResponse response = service.update(poId, req);

        assertNotNull(response);
        assertEquals("REF-UPDATED", response.supplierReference());
        verify(purchaseOrders).saveAndFlush(po);
    }

    @Test
    void confirm_Success() {
        UUID poId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setSupplierId(supplierId);
        po.setWarehouseId(warehouseId);

        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(UUID.randomUUID());
        line.setItemId(itemId);
        line.setOrderedQty(BigDecimal.TEN);
        po.addLine(line);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(warehouses.getById(warehouseId)).thenReturn(activeWarehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(activeItem));
        when(purchaseOrders.saveAndFlush(any(PurchaseOrder.class))).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderResponse response = service.confirm(poId, actorId);

        assertEquals(PurchaseOrderStatus.CONFIRMED, response.status());
        verify(purchaseOrders).saveAndFlush(po);
    }

    @Test
    void confirm_NoLines_ThrowsException() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.DRAFT);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.confirm(poId, UUID.randomUUID()));
        assertEquals("PO_NO_LINES", ex.getCode());
    }

    @Test
    void cancel_Success() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.CONFIRMED);
        po.setSupplierId(supplierId);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(purchaseOrders.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderResponse response = service.cancel(poId, "Supplier cancelled");

        assertEquals(PurchaseOrderStatus.CANCELLED, response.status());
    }

    @Test
    void cancel_PartiallyReceived_ThrowsConflictException() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.cancel(poId, "Cancel"));
        assertEquals("PO_HAS_RECEIPTS", ex.getCode());
    }

    @Test
    void close_Success() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        po.setSupplierId(supplierId);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(purchaseOrders.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderResponse response = service.close(poId, UUID.randomUUID());

        assertEquals(PurchaseOrderStatus.CLOSED, response.status());
    }

    @Test
    void close_ConfirmedStatus_ThrowsConflictException() {
        UUID poId = UUID.randomUUID();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.CONFIRMED);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.close(poId, UUID.randomUUID()));
        assertEquals("PO_INVALID_STATE", ex.getCode());
    }

    @Test
    void changeTolerance_Success() {
        UUID poId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        po.setStatus(PurchaseOrderStatus.CONFIRMED);
        po.setSupplierId(supplierId);

        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(lineId);
        line.setItemId(itemId);
        line.setOrderedQty(BigDecimal.TEN);
        line.setTolerancePercent(BigDecimal.ZERO);
        po.addLine(line);

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(purchaseOrders.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        PurchaseOrderResponse response = service.changeTolerance(poId, lineId, BigDecimal.valueOf(10));

        assertNotNull(response);
        verify(purchaseOrders).saveAndFlush(po);
    }

    @Test
    void scan_Success() {
        UUID poId = UUID.randomUUID();
        String barcode = "BAR001";

        PurchaseOrder po = new PurchaseOrder();
        po.setId(poId);
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setId(UUID.randomUUID());
        line.setLineNo(1);
        line.setItemId(itemId);
        line.setOrderedQty(BigDecimal.TEN);
        line.setAcceptedQty(BigDecimal.ZERO);
        po.addLine(line);

        when(purchaseOrders.findWithLinesById(poId)).thenReturn(Optional.of(po));
        when(items.resolveBarcode(barcode)).thenReturn(new BarcodeResolution(activeItem, "PCS", BigDecimal.ONE));

        PurchaseOrderLineScanResponse response = service.scan(poId, barcode);

        assertNotNull(response);
        assertEquals("PCS", response.uom());
        assertEquals(BigDecimal.ONE, response.factorToBase());
    }

    @Test
    void search_Success() {
        PurchaseOrderFilter filter = new PurchaseOrderFilter(
                warehouseId, null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        PurchaseOrder po = new PurchaseOrder();
        po.setId(UUID.randomUUID());
        po.setSupplierId(supplierId);
        po.setWarehouseId(warehouseId);
        po.setOrderDate(LocalDate.now());
        po.setStatus(PurchaseOrderStatus.DRAFT);

        Page<PurchaseOrder> page = new PageImpl<>(List.of(po));
        when(purchaseOrders.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(suppliers.getById(supplierId)).thenReturn(activeSupplier);
        when(orderLines.totalsByOrder(List.of(po.getId()))).thenReturn(List.of());

        PageResponse<PurchaseOrderSummaryResponse> res = service.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.content().size());
    }
}

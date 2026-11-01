package rw.afriteck.wms.inbound.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.entity.GoodsReceipt;
import rw.afriteck.wms.inbound.entity.GoodsReceiptLine;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.enums.PutawayStatus;
import rw.afriteck.wms.inbound.enums.ReceiptMode;
import rw.afriteck.wms.inbound.repository.GoodsReceiptLineRepository;
import rw.afriteck.wms.inbound.repository.GoodsReceiptRepository;
import rw.afriteck.wms.inbound.repository.PurchaseOrderRepository;
import rw.afriteck.wms.inbound.support.PutawaySuggester;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskLineStatus;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoodsReceiptServiceImplTest {

    @Mock
    private PurchaseOrderRepository purchaseOrders;
    @Mock
    private GoodsReceiptRepository goodsReceipts;
    @Mock
    private GoodsReceiptLineRepository receiptLines;
    @Mock
    private WarehouseService warehouses;
    @Mock
    private LocationService locations;
    @Mock
    private ItemService items;
    @Mock
    private InventoryService inventory;
    @Mock
    private TaskService tasks;
    @Mock
    private PutawaySuggester suggester;
    @Mock
    private WorkerLookup workers;
    @Mock
    private Clock clock;

    @InjectMocks
    private GoodsReceiptServiceImpl service;

    private UUID poId;
    private UUID warehouseId;
    private UUID itemId;
    private UUID poLineId;
    private UUID receivingBinId;
    private UUID storageBinId;

    private WarehouseResponse warehouse;
    private ItemResponse item;
    private LocationResponse receivingBin;
    private LocationResponse storageBin;
    private PurchaseOrder po;
    private PurchaseOrderLine poLine;

    @BeforeEach
    void setUp() {
        poId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        poLineId = UUID.randomUUID();
        receivingBinId = UUID.randomUUID();
        storageBinId = UUID.randomUUID();

        warehouse = new WarehouseResponse(warehouseId, "WH1", "Warehouse 1", "Addr", "Kigali", "Rwanda", true);
        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Cat", "PCS", TrackingType.NONE, false, true);
        receivingBin = new LocationResponse(receivingBinId, warehouseId, null, "Receiving Bin", "RCV01", LocationType.BIN, LocationUsage.RECEIVING, null, null, true);
        storageBin = new LocationResponse(storageBinId, warehouseId, null, "Storage Bin", "STO01", LocationType.BIN, LocationUsage.STORAGE, null, null, true);

        po = new PurchaseOrder();
        po.setId(poId);
        po.setPoNumber("PO-000001");
        po.setWarehouseId(warehouseId);
        po.setStatus(PurchaseOrderStatus.CONFIRMED);

        poLine = new PurchaseOrderLine();
        poLine.setId(poLineId);
        poLine.setItemId(itemId);
        poLine.setOrderedQty(BigDecimal.TEN);
        poLine.setAcceptedQty(BigDecimal.ZERO);
        poLine.setRejectedQty(BigDecimal.ZERO);
        poLine.setTolerancePercent(BigDecimal.ZERO);
        po.addLine(poLine);

        lenient().when(clock.instant()).thenReturn(Instant.now());
    }

    @Test
    void receive_PutawayMode_Success() {
        UUID actorId = UUID.randomUUID();
        ReceiveGoodsLineRequest lineReq = new ReceiveGoodsLineRequest(
                poLineId, "PCS", BigDecimal.TEN, BigDecimal.ZERO, null, null, null, null, null);
        ReceiveGoodsRequest req = new ReceiveGoodsRequest(
                ReceiptMode.PUTAWAY, receivingBinId, null, "DN123", "Notes", List.of(lineReq));

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(warehouses.getById(warehouseId)).thenReturn(warehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locations.requireStorableBin(receivingBinId)).thenReturn(receivingBin);
        when(goodsReceipts.nextReceiptNumber()).thenReturn(1L);

        UUID taskId = UUID.randomUUID();
        UUID taskLineId = UUID.randomUUID();
        TaskLineResponse taskLine = new TaskLineResponse(
                taskLineId, 1, itemId, "SKU1", "Item 1", null, null,
                receivingBinId, "RCV01", storageBinId, "STO01",
                BigDecimal.TEN, BigDecimal.ZERO, TaskLineStatus.PENDING, UUID.randomUUID(), null);
        TaskResponse taskResponse = new TaskResponse(
                taskId, 1L, "TASK-001", TaskType.PUTAWAY, TaskStatus.ASSIGNED, TaskPriority.NORMAL,
                warehouseId, null, "GOODS_RECEIPT", UUID.randomUUID(), "GRN-000001",
                actorId, "Actor", Instant.now(), Instant.now(), null, null,
                null, null, Instant.now(), List.of(taskLine));
        when(tasks.create(any())).thenReturn(taskResponse);

        when(goodsReceipts.saveAndFlush(any(GoodsReceipt.class))).thenAnswer(i -> {
            GoodsReceipt gr = i.getArgument(0);
            if (gr.getId() == null) gr.setId(UUID.randomUUID());
            return gr;
        });

        GoodsReceiptResponse response = service.receive(poId, req, actorId);

        assertNotNull(response);
        verify(inventory).post(any());
        verify(tasks).create(any());
        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
    }

    @Test
    void receive_DirectMode_Success() {
        UUID actorId = UUID.randomUUID();
        ReceiveGoodsLineRequest lineReq = new ReceiveGoodsLineRequest(
                poLineId, "PCS", BigDecimal.TEN, BigDecimal.ZERO, null, null, null, null, storageBinId);
        ReceiveGoodsRequest req = new ReceiveGoodsRequest(
                ReceiptMode.DIRECT, null, null, "DN123", "Notes", List.of(lineReq));

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(warehouses.getById(warehouseId)).thenReturn(warehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locations.requireStorableBin(storageBinId)).thenReturn(storageBin);
        when(goodsReceipts.nextReceiptNumber()).thenReturn(1L);

        when(goodsReceipts.saveAndFlush(any(GoodsReceipt.class))).thenAnswer(i -> {
            GoodsReceipt gr = i.getArgument(0);
            if (gr.getId() == null) gr.setId(UUID.randomUUID());
            return gr;
        });

        GoodsReceiptResponse response = service.receive(poId, req, actorId);

        assertNotNull(response);
        verify(inventory).post(any());
        verify(tasks, never()).create(any());
    }

    @Test
    void receive_NoActor_ThrowsException() {
        ReceiveGoodsRequest req = new ReceiveGoodsRequest(
                ReceiptMode.PUTAWAY, receivingBinId, null, null, null, List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(poId, req, null));
        assertEquals("ACTOR_REQUIRED", ex.getCode());
    }

    @Test
    void receive_ExceedsTolerance_ThrowsException() {
        UUID actorId = UUID.randomUUID();
        ReceiveGoodsLineRequest lineReq = new ReceiveGoodsLineRequest(
                poLineId, "PCS", BigDecimal.valueOf(15), BigDecimal.ZERO, null, null, null, null, null);
        ReceiveGoodsRequest req = new ReceiveGoodsRequest(
                ReceiptMode.PUTAWAY, receivingBinId, null, null, null, List.of(lineReq));

        when(purchaseOrders.findByIdForUpdate(poId)).thenReturn(Optional.of(po));
        when(warehouses.getById(warehouseId)).thenReturn(warehouse);
        when(items.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.receive(poId, req, actorId));
        assertEquals("RECEIPT_EXCEEDS_TOLERANCE", ex.getCode());
    }

    @Test
    void confirmPutaway_Success() {
        UUID receiptId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID taskLineId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setId(receiptId);
        receipt.setWarehouseId(warehouseId);
        receipt.setTaskId(taskId);
        receipt.setPurchaseOrder(po);
        receipt.setReceivedBy(actorId);

        GoodsReceiptLine line = new GoodsReceiptLine();
        line.setId(lineId);
        line.setGoodsReceipt(receipt);
        line.setPurchaseOrderLine(poLine);
        line.setItemId(itemId);
        line.setAcceptedQty(BigDecimal.TEN);
        line.setReceivingLocationId(receivingBinId);
        line.setSuggestedLocationId(storageBinId);
        line.setPutawayStatus(PutawayStatus.PENDING);
        line.setTaskLineId(taskLineId);
        receipt.addLine(line);

        when(receiptLines.findByIdForUpdate(lineId)).thenReturn(Optional.of(line));
        TaskResponse task = new TaskResponse(
                taskId, 1L, "TASK1", TaskType.PUTAWAY, TaskStatus.IN_PROGRESS, TaskPriority.NORMAL,
                warehouseId, null, "GOODS_RECEIPT", receiptId, "GRN-000001",
                actorId, "Actor", Instant.now(), Instant.now(), null, null,
                null, null, Instant.now(), List.of());
        when(tasks.getById(taskId)).thenReturn(task);
        when(locations.requireStorableBin(storageBinId)).thenReturn(storageBin);

        ConfirmPutawayRequest req = new ConfirmPutawayRequest(storageBinId, BigDecimal.TEN, null);

        GoodsReceiptResponse response = service.confirmPutaway(receiptId, lineId, req, actorId, false);

        assertNotNull(response);
        assertEquals(PutawayStatus.DONE, line.getPutawayStatus());
        verify(inventory).post(any());
        verify(tasks).completeLine(eq(taskId), eq(taskLineId), argThat(qty -> qty.compareTo(BigDecimal.TEN) == 0), isNull());
    }

    @Test
    void confirmPutaway_AssignedToOtherUser_ThrowsForbidden() {
        UUID receiptId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setId(receiptId);
        receipt.setTaskId(taskId);
        GoodsReceiptLine line = new GoodsReceiptLine();
        line.setId(lineId);
        line.setGoodsReceipt(receipt);
        line.setPutawayStatus(PutawayStatus.PENDING);

        when(receiptLines.findByIdForUpdate(lineId)).thenReturn(Optional.of(line));
        TaskResponse task = new TaskResponse(
                taskId, 1L, "TASK1", TaskType.PUTAWAY, TaskStatus.IN_PROGRESS, TaskPriority.NORMAL,
                warehouseId, null, "GOODS_RECEIPT", receiptId, "GRN-000001",
                otherUserId, "Other", Instant.now(), Instant.now(), null, null,
                null, null, Instant.now(), List.of());
        when(tasks.getById(taskId)).thenReturn(task);

        assertThrows(ForbiddenException.class, () -> service.confirmPutaway(receiptId, lineId, null, actorId, false));
    }
}

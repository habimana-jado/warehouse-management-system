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
import rw.afriteck.wms.inventory.entity.StockTransfer;
import rw.afriteck.wms.inventory.entity.StockTransferLine;
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.inventory.repository.LotRepository;
import rw.afriteck.wms.inventory.repository.StockTransferRepository;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.operations.dtos.TaskLineResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
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
class TransferServiceImplTest {

    @Mock
    private StockTransferRepository transfers;
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
    private Clock clock;

    @InjectMocks
    private TransferServiceImpl service;

    private UUID sourceWhId;
    private UUID destWhId;
    private UUID itemId;
    private UUID fromLocId;
    private UUID toLocId;
    private UUID transferId;
    private WarehouseResponse sourceWh;
    private WarehouseResponse destWh;
    private ItemResponse item;
    private LocationResponse fromLoc;
    private LocationResponse toLoc;
    private StockTransfer transfer;

    @BeforeEach
    void setUp() {
        sourceWhId = UUID.randomUUID();
        destWhId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        fromLocId = UUID.randomUUID();
        toLocId = UUID.randomUUID();
        transferId = UUID.randomUUID();

        sourceWh = new WarehouseResponse(sourceWhId, "WH1", "Source WH", "Addr", "City", "Country", true);
        destWh = new WarehouseResponse(destWhId, "WH2", "Dest WH", "Addr", "City", "Country", true);
        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.NONE, false, true);
        fromLoc = new LocationResponse(fromLocId, sourceWhId, null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
        toLoc = new LocationResponse(toLocId, destWhId, null, "Bin 2", "BIN2", LocationType.BIN, LocationUsage.STORAGE, null, null, true);

        transfer = new StockTransfer();
        transfer.setId(transferId);
        transfer.setType(TransferType.INTER_WAREHOUSE);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setSourceWarehouseId(sourceWhId);
        transfer.setDestinationWarehouseId(destWhId);
        transfer.setTransferNumber("TRF-000001");
    }

    @Test
    void create_BinTransfer_Immediate_Success() {
        CreateTransferLineRequest lineReq = new CreateTransferLineRequest(itemId, null, null, fromLocId, toLocId, BigDecimal.TEN);
        CreateTransferRequest req = new CreateTransferRequest(TransferType.BIN, sourceWhId, sourceWhId, true, null, null, "Notes", List.of(lineReq));

        LocationResponse binToLoc = new LocationResponse(toLocId, sourceWhId, null, "Bin 2", "BIN2", LocationType.BIN, LocationUsage.STORAGE, null, null, true);

        when(warehouseService.getById(sourceWhId)).thenReturn(sourceWh);
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.requireStorableBin(fromLocId)).thenReturn(fromLoc);
        when(locationService.requireStorableBin(toLocId)).thenReturn(binToLoc);
        when(transfers.nextNumber()).thenReturn(1L);
        when(clock.instant()).thenReturn(Instant.now());
        when(transfers.saveAndFlush(any(StockTransfer.class))).thenReturn(transfer);

        TransferResponse res = service.create(req);

        assertNotNull(res);
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void create_InterWarehouse_ImmediateNotAllowed_ThrowsBusinessException() {
        CreateTransferLineRequest lineReq = new CreateTransferLineRequest(itemId, null, null, fromLocId, null, BigDecimal.TEN);
        CreateTransferRequest req = new CreateTransferRequest(TransferType.INTER_WAREHOUSE, sourceWhId, destWhId, true, null, null, "Notes", List.of(lineReq));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("TRANSFER_IMMEDIATE_NOT_ALLOWED", ex.getCode());
    }

    @Test
    void confirmLine_Success() {
        UUID lineId = UUID.randomUUID();
        StockTransferLine line = new StockTransferLine();
        line.setId(lineId);
        line.setTransfer(transfer);
        line.setLineNo(1);
        line.setItemId(itemId);
        line.setFromLocationId(fromLocId);
        line.setQuantity(BigDecimal.TEN);
        transfer.getLines().add(line);

        when(transfers.findWithLinesById(transferId)).thenReturn(Optional.of(transfer));
        TaskResponse task = new TaskResponse(UUID.randomUUID(), 1L, "TASK-001", TaskType.TRANSFER, TaskStatus.IN_PROGRESS, null, sourceWhId, null,
                "TRF", transferId, "TRF-000001", UUID.randomUUID(), null, null, null, null, null, null, null, Instant.now(), List.of(
                        new TaskLineResponse(UUID.randomUUID(), 1, itemId, "SKU1", "Item 1", null, null, fromLocId, "BIN1", null, null, BigDecimal.TEN, BigDecimal.ZERO, null, lineId, null)
        ));

        when(taskService.findByReference(any(), eq(transferId))).thenReturn(List.of(task));
        when(clock.instant()).thenReturn(Instant.now());
        when(transfers.saveAndFlush(transfer)).thenReturn(transfer);

        UUID actorId = task.assigneeId();
        ConfirmTransferLineRequest req = new ConfirmTransferLineRequest(BigDecimal.TEN, null);

        TransferResponse res = service.confirmLine(transferId, lineId, req, actorId, false);

        assertNotNull(res);
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void ship_Success() {
        StockTransferLine line = new StockTransferLine();
        line.setId(UUID.randomUUID());
        line.setTransfer(transfer);
        line.setItemId(itemId);
        line.setFromLocationId(fromLocId);
        line.setQuantity(BigDecimal.TEN);
        transfer.getLines().add(line);

        when(transfers.findWithLinesById(transferId)).thenReturn(Optional.of(transfer));
        when(warehouseService.getById(destWhId)).thenReturn(destWh);
        when(clock.instant()).thenReturn(Instant.now());
        when(transfers.saveAndFlush(transfer)).thenReturn(transfer);

        TransferResponse res = service.ship(transferId);

        assertNotNull(res);
        assertEquals(TransferStatus.IN_TRANSIT, transfer.getStatus());
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void receive_Success() {
        transfer.setStatus(TransferStatus.IN_TRANSIT);
        UUID lineId = UUID.randomUUID();
        StockTransferLine line = new StockTransferLine();
        line.setId(lineId);
        line.setTransfer(transfer);
        line.setItemId(itemId);
        line.setFromLocationId(fromLocId);
        line.setQuantity(BigDecimal.TEN);
        line.setShippedQty(BigDecimal.TEN);
        line.setReceivedQty(BigDecimal.ZERO);
        transfer.getLines().add(line);

        when(transfers.findWithLinesById(transferId)).thenReturn(Optional.of(transfer));
        when(locationService.requireStorableBin(toLocId)).thenReturn(toLoc);
        when(clock.instant()).thenReturn(Instant.now());
        when(transfers.saveAndFlush(transfer)).thenReturn(transfer);

        ReceiveTransferRequest req = new ReceiveTransferRequest(List.of(new ReceiveLineRequest(lineId, toLocId, BigDecimal.TEN)), false, null);

        TransferResponse res = service.receive(transferId, req);

        assertNotNull(res);
        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        verify(inventory).post(any(PostStockRequest.class));
    }

    @Test
    void cancel_Success() {
        when(transfers.findWithLinesById(transferId)).thenReturn(Optional.of(transfer));
        when(clock.instant()).thenReturn(Instant.now());
        when(transfers.saveAndFlush(transfer)).thenReturn(transfer);

        TransferResponse res = service.cancel(transferId, "Cancelled");

        assertNotNull(res);
        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
        assertEquals("Cancelled", transfer.getCancelReason());
    }

    @Test
    void search_Success() {
        TransferFilter filter = new TransferFilter(sourceWhId, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockTransfer> page = new PageImpl<>(List.of(transfer), pageable, 1);

        when(transfers.search(eq(sourceWhId), anySet(), any(), any(Pageable.class))).thenReturn(page);

        PageResponse<TransferSummaryResponse> res = service.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }
}

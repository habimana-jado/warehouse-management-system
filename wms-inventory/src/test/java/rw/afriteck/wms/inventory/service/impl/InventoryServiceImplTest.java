package rw.afriteck.wms.inventory.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.inventory.dtos.PostStockLine;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.dtos.PostStockResponse;
import rw.afriteck.wms.inventory.entity.StockBalance;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.repository.LotRepository;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private StockBalanceRepository balances;
    @Mock
    private StockMovementRepository movements;
    @Mock
    private LotRepository lots;
    @Mock
    private ItemService itemService;
    @Mock
    private LocationService locationService;
    @Mock
    private Clock clock;

    @InjectMocks
    private InventoryServiceImpl service;

    private UUID warehouseId;
    private UUID itemId;
    private UUID locationId;
    private ItemResponse item;
    private LocationResponse location;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();

        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.NONE, false, true);
        location = new LocationResponse(locationId, warehouseId, null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
    }

    @Test
    void post_Success_UntrackedReceipt() {
        PostStockLine line = new PostStockLine(MovementType.RECEIPT, locationId, itemId, null, null, null, BigDecimal.TEN, BigDecimal.valueOf(50));
        PostStockRequest req = new PostStockRequest(null, null, "PO", UUID.randomUUID(), "PO-001", "Receipt note", List.of(line));

        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.requireStorableBin(locationId)).thenReturn(location);
        when(clock.instant()).thenReturn(Instant.now());
        when(balances.lockUntracked(locationId, itemId)).thenReturn(Optional.empty());

        PostStockResponse res = service.post(req);

        assertNotNull(res);
        assertNotNull(res.transactionId());
        assertEquals(1, res.movements().size());
        verify(balances).save(any(StockBalance.class));
        verify(movements).save(any());
        verify(balances).flush();
    }

    @Test
    void post_NoLines_ThrowsBusinessException() {
        PostStockRequest req = new PostStockRequest(null, null, null, null, null, null, List.of());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.post(req));
        assertEquals("STOCK_NO_LINES", ex.getCode());
    }

    @Test
    void post_InsufficientStock_ThrowsBusinessException() {
        PostStockLine line = new PostStockLine(MovementType.ISSUE, locationId, itemId, null, null, null, BigDecimal.TEN.negate(), null);
        PostStockRequest req = new PostStockRequest(null, null, null, null, null, null, List.of(line));

        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.requireStorableBin(locationId)).thenReturn(location);

        StockBalance currentBalance = new StockBalance();
        currentBalance.setWarehouseId(warehouseId);
        currentBalance.setLocationId(locationId);
        currentBalance.setItemId(itemId);
        currentBalance.setQuantity(BigDecimal.valueOf(5));

        when(balances.lockUntracked(locationId, itemId)).thenReturn(Optional.of(currentBalance));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.post(req));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
    }

    @Test
    void post_DuplicateSerialInStock_ThrowsConflictException() {
        ItemResponse serialItem = new ItemResponse(itemId, "SKU2", "Item 2", null, null, "Category", "PCS", TrackingType.SERIAL, false, true);
        PostStockLine line = new PostStockLine(MovementType.RECEIPT, locationId, itemId, null, null, "SN001", BigDecimal.ONE, null);
        PostStockRequest req = new PostStockRequest(null, null, null, null, null, null, List.of(line));

        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(serialItem));
        when(locationService.requireStorableBin(locationId)).thenReturn(location);

        StockBalance existingSerial = new StockBalance();
        when(balances.findByItemIdAndSerialNumberAndQuantityGreaterThan(itemId, "SN001", BigDecimal.ZERO))
                .thenReturn(Optional.of(existingSerial));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.post(req));
        assertEquals("SERIAL_ALREADY_IN_STOCK", ex.getCode());
    }

    @Test
    void post_LotNotFoundForNegativeDelta_ThrowsBusinessException() {
        ItemResponse lotItem = new ItemResponse(itemId, "SKU3", "Item 3", null, null, "Category", "PCS", TrackingType.LOT, false, true);
        PostStockLine line = new PostStockLine(MovementType.ISSUE, locationId, itemId, "LOT99", null, null, BigDecimal.ONE.negate(), null);
        PostStockRequest req = new PostStockRequest(null, null, null, null, null, null, List.of(line));

        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(lotItem));
        when(locationService.requireStorableBin(locationId)).thenReturn(location);
        when(lots.findByItemIdAndLotNumberIn(itemId, Set.of("LOT99"))).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.post(req));
        assertEquals("LOT_NOT_FOUND", ex.getCode());
    }

    @Test
    void check_Success() {
        PostStockLine line = new PostStockLine(MovementType.RECEIPT, locationId, itemId, null, null, null, BigDecimal.TEN, null);
        PostStockRequest req = new PostStockRequest(null, null, null, null, null, null, List.of(line));

        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.requireStorableBin(locationId)).thenReturn(location);
        when(balances.quantityUntracked(locationId, itemId)).thenReturn(Optional.of(BigDecimal.ZERO));

        assertDoesNotThrow(() -> service.check(req));
    }

    @Test
    void check_InsufficientStock_ThrowsBusinessException() {
        PostStockLine line = new PostStockLine(MovementType.ISSUE, locationId, itemId, null, null, null, BigDecimal.TEN.negate(), null);
        PostStockRequest req = new PostStockRequest(null, null, null, null, null, null, List.of(line));

        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.requireStorableBin(locationId)).thenReturn(location);
        when(balances.quantityUntracked(locationId, itemId)).thenReturn(Optional.of(BigDecimal.valueOf(2)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.check(req));
        assertEquals("INSUFFICIENT_STOCK", ex.getCode());
    }
}

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
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.entity.StockBalance;
import rw.afriteck.wms.inventory.entity.StockMovement;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockMovementRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceImplTest {

    @Mock
    private StockBalanceRepository balances;
    @Mock
    private StockMovementRepository movements;
    @Mock
    private ItemService itemService;
    @Mock
    private LocationService locationService;

    @InjectMocks
    private InventoryQueryServiceImpl service;

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
    void findAvailable_Success() {
        StockBalance balance = new StockBalance();
        balance.setWarehouseId(warehouseId);
        balance.setItemId(itemId);
        balance.setLocationId(locationId);
        balance.setQuantity(BigDecimal.TEN);

        when(balances.findAvailable(warehouseId, itemId)).thenReturn(List.of(balance));
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.findAllByIds(Set.of(locationId))).thenReturn(List.of(location));

        List<StockLevelResponse> res = service.findAvailable(warehouseId, itemId);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(BigDecimal.TEN, res.get(0).quantity());
    }

    @Test
    void getOnHand_SingleItem_Success() {
        when(balances.sumQuantity(warehouseId, itemId)).thenReturn(BigDecimal.valueOf(15));

        BigDecimal total = service.getOnHand(warehouseId, itemId);

        assertEquals(BigDecimal.valueOf(15), total);
    }

    @Test
    void getOnHand_MultipleItems_Success() {
        UUID item2Id = UUID.randomUUID();
        StockBalanceRepository.ItemQuantity sum1 = mock(StockBalanceRepository.ItemQuantity.class);
        when(sum1.getItemId()).thenReturn(itemId);
        when(sum1.getQuantity()).thenReturn(BigDecimal.TEN);

        when(balances.sumByItems(eq(warehouseId), anyList())).thenReturn(List.of(sum1));

        Map<UUID, BigDecimal> res = service.getOnHand(warehouseId, List.of(itemId, item2Id));

        assertNotNull(res);
        assertEquals(2, res.size());
        assertEquals(BigDecimal.TEN, res.get(itemId));
        assertEquals(BigDecimal.ZERO, res.get(item2Id));
    }

    @Test
    void search_Success() {
        StockFilter filter = new StockFilter(warehouseId, locationId, itemId, null, null, false);
        Pageable pageable = PageRequest.of(0, 10);

        StockBalance balance = new StockBalance();
        balance.setWarehouseId(warehouseId);
        balance.setItemId(itemId);
        balance.setLocationId(locationId);
        balance.setQuantity(BigDecimal.TEN);

        Page<StockBalance> page = new PageImpl<>(List.of(balance), pageable, 1);
        when(balances.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.findAllByIds(Set.of(locationId))).thenReturn(List.of(location));

        PageResponse<StockLevelResponse> res = service.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }

    @Test
    void search_MissingWarehouse_ThrowsBusinessException() {
        StockFilter filter = new StockFilter(null, null, null, null, null, false);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.search(filter, PageRequest.of(0, 10)));
        assertEquals("WAREHOUSE_REQUIRED", ex.getCode());
    }

    @Test
    void findInLocation_Success() {
        when(locationService.getById(locationId)).thenReturn(location);

        StockBalance balance = new StockBalance();
        balance.setWarehouseId(warehouseId);
        balance.setItemId(itemId);
        balance.setLocationId(locationId);
        balance.setQuantity(BigDecimal.TEN);

        when(balances.findInLocation(locationId)).thenReturn(List.of(balance));
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.findAllByIds(Set.of(locationId))).thenReturn(List.of(location));

        List<StockLevelResponse> res = service.findInLocation(warehouseId, locationId);

        assertNotNull(res);
        assertEquals(1, res.size());
    }

    @Test
    void findInLocation_WrongWarehouse_ThrowsBusinessException() {
        LocationResponse wrongWhLocation = new LocationResponse(locationId, UUID.randomUUID(), null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);
        when(locationService.getById(locationId)).thenReturn(wrongWhLocation);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.findInLocation(warehouseId, locationId));
        assertEquals("LOCATION_WRONG_WAREHOUSE", ex.getCode());
    }

    @Test
    void history_Success() {
        Instant now = Instant.now();
        MovementFilter filter = new MovementFilter(warehouseId, itemId, locationId, null, null, null, now.minusSeconds(3600), now, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        StockMovement movement = new StockMovement();
        movement.setTransactionId(UUID.randomUUID());
        movement.setType(MovementType.RECEIPT);
        movement.setOccurredAt(now);
        movement.setWarehouseId(warehouseId);
        movement.setItemId(itemId);
        movement.setLocationId(locationId);
        movement.setQuantity(BigDecimal.TEN);

        Page<StockMovement> page = new PageImpl<>(List.of(movement), pageable, 1);
        when(movements.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));
        when(locationService.findAllByIds(Set.of(locationId))).thenReturn(List.of(location));

        PageResponse<MovementHistoryResponse> res = service.history(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }

    @Test
    void history_InvalidDateRange_ThrowsBusinessException() {
        Instant now = Instant.now();
        MovementFilter filter = new MovementFilter(warehouseId, null, null, null, null, null, now, now.minusSeconds(3600), null, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.history(filter, PageRequest.of(0, 10)));
        assertEquals("INVALID_DATE_RANGE", ex.getCode());
    }
}

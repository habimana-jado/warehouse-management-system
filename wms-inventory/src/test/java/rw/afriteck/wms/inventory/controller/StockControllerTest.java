package rw.afriteck.wms.inventory.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private InventoryQueryService queries;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private StockController controller;

    private UUID warehouseId;
    private UUID itemId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<StockLevelResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(queries.search(any(StockFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<StockLevelResponse> res = controller.search(warehouseId, locationId, itemId, null, null, false, pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void inLocation_Success() {
        when(queries.findInLocation(warehouseId, locationId)).thenReturn(List.of());

        List<StockLevelResponse> res = controller.inLocation(locationId, warehouseId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void availability_Success() {
        when(queries.findAvailable(warehouseId, itemId)).thenReturn(List.of());

        List<StockLevelResponse> res = controller.availability(itemId, warehouseId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void movements_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<MovementHistoryResponse> pageResponse = new PageResponse<>(List.of(), 0, 10, 0, 0);
        when(queries.history(any(MovementFilter.class), eq(pageable))).thenReturn(pageResponse);

        PageResponse<MovementHistoryResponse> res = controller.movements(warehouseId, itemId, locationId, null, null, null, null, null, null, null, pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}

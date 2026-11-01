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
import rw.afriteck.wms.inventory.dtos.CreateReorderPolicyRequest;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;
import rw.afriteck.wms.inventory.service.ReorderPolicyService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderPolicyControllerTest {

    @Mock
    private ReorderPolicyService service;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private ReorderPolicyController controller;

    private UUID warehouseId;
    private UUID itemId;
    private UUID policyId;
    private ReorderPolicyResponse response;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        response = new ReorderPolicyResponse(policyId, 1L, warehouseId, itemId, "SKU1", "Item 1", "PCS", BigDecimal.TEN, BigDecimal.valueOf(50), BigDecimal.valueOf(100), true);
    }

    @Test
    void create_Success() {
        CreateReorderPolicyRequest req = new CreateReorderPolicyRequest(warehouseId, itemId, BigDecimal.TEN, BigDecimal.valueOf(50), BigDecimal.valueOf(100));
        when(service.create(req)).thenReturn(response);

        ReorderPolicyResponse res = controller.create(req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).create(req);
    }

    @Test
    void update_Success() {
        UpdateReorderPolicyRequest req = new UpdateReorderPolicyRequest(BigDecimal.valueOf(15), BigDecimal.valueOf(60), BigDecimal.valueOf(120), false, true);
        when(service.warehouseIdOf(policyId)).thenReturn(warehouseId);
        when(service.update(policyId, req)).thenReturn(response);

        ReorderPolicyResponse res = controller.update(policyId, req);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).update(policyId, req);
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<ReorderPolicyResponse> pageResponse = new PageResponse<>(List.of(response), 0, 10, 1, 1);
        when(service.search(warehouseId, true, pageable)).thenReturn(pageResponse);

        PageResponse<ReorderPolicyResponse> res = controller.search(warehouseId, true, pageable);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void get_Success() {
        when(service.warehouseIdOf(policyId)).thenReturn(warehouseId);
        when(service.getById(policyId)).thenReturn(response);

        ReorderPolicyResponse res = controller.get(policyId);

        assertNotNull(res);
        verify(userContext).requireWarehouseAccess(warehouseId);
    }

    @Test
    void lowStock_Success() {
        LowStockResponse lowStock = new LowStockResponse(itemId, "SKU1", "Item 1", "PCS", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(50), BigDecimal.valueOf(100), BigDecimal.valueOf(50));
        when(service.findBelowMinimum(warehouseId)).thenReturn(List.of(lowStock));

        List<LowStockResponse> res = controller.lowStock(warehouseId);

        assertNotNull(res);
        assertEquals(1, res.size());
        verify(userContext).requireWarehouseAccess(warehouseId);
    }
}

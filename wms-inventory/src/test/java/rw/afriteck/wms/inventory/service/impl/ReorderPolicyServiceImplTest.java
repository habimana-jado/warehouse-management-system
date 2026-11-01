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
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.CreateReorderPolicyRequest;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;
import rw.afriteck.wms.inventory.entity.ReorderPolicy;
import rw.afriteck.wms.inventory.repository.ReorderPolicyRepository;
import rw.afriteck.wms.inventory.service.InventoryQueryService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderPolicyServiceImplTest {

    @Mock
    private ReorderPolicyRepository policies;
    @Mock
    private InventoryQueryService queries;
    @Mock
    private WarehouseService warehouseService;
    @Mock
    private ItemService itemService;

    @InjectMocks
    private ReorderPolicyServiceImpl service;

    private UUID warehouseId;
    private UUID itemId;
    private UUID policyId;
    private WarehouseResponse warehouse;
    private ItemResponse item;
    private ReorderPolicy policy;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        warehouse = new WarehouseResponse(warehouseId, "WH1", "Main Warehouse", "Addr", "City", "Country", true);
        item = new ItemResponse(itemId, "SKU1", "Item 1", "EAN1", null, "Category", "PCS", TrackingType.NONE, false, true);

        policy = new ReorderPolicy();
        policy.setId(policyId);
        policy.setWarehouseId(warehouseId);
        policy.setItemId(itemId);
        policy.setMinQty(BigDecimal.TEN);
        policy.setReorderQty(BigDecimal.valueOf(50));
        policy.setMaxQty(BigDecimal.valueOf(100));
        policy.setActive(true);
    }

    @Test
    void create_Success() {
        CreateReorderPolicyRequest req = new CreateReorderPolicyRequest(warehouseId, itemId, BigDecimal.TEN, BigDecimal.valueOf(50), BigDecimal.valueOf(100));

        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(itemService.getById(itemId)).thenReturn(item);
        when(policies.existsByWarehouseIdAndItemId(warehouseId, itemId)).thenReturn(false);
        when(policies.saveAndFlush(any(ReorderPolicy.class))).thenReturn(policy);

        ReorderPolicyResponse res = service.create(req);

        assertNotNull(res);
        assertEquals(warehouseId, res.warehouseId());
        assertEquals(itemId, res.itemId());
    }

    @Test
    void create_DuplicatePolicy_ThrowsConflictException() {
        CreateReorderPolicyRequest req = new CreateReorderPolicyRequest(warehouseId, itemId, BigDecimal.TEN, BigDecimal.valueOf(50), BigDecimal.valueOf(100));

        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(itemService.getById(itemId)).thenReturn(item);
        when(policies.existsByWarehouseIdAndItemId(warehouseId, itemId)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(req));
        assertEquals("REORDER_POLICY_EXISTS", ex.getCode());
    }

    @Test
    void create_InvalidLevels_ThrowsBusinessException() {
        CreateReorderPolicyRequest req = new CreateReorderPolicyRequest(warehouseId, itemId, BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.TEN);

        when(warehouseService.getById(warehouseId)).thenReturn(warehouse);
        when(itemService.getById(itemId)).thenReturn(item);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertEquals("REORDER_LEVELS_INVALID", ex.getCode());
    }

    @Test
    void update_Success() {
        UpdateReorderPolicyRequest req = new UpdateReorderPolicyRequest(BigDecimal.valueOf(15), BigDecimal.valueOf(60), BigDecimal.valueOf(120), false, true);

        when(policies.findById(policyId)).thenReturn(Optional.of(policy));
        when(policies.saveAndFlush(policy)).thenReturn(policy);
        when(itemService.getById(itemId)).thenReturn(item);

        ReorderPolicyResponse res = service.update(policyId, req);

        assertNotNull(res);
        assertEquals(BigDecimal.valueOf(15), policy.getMinQty());
        assertEquals(BigDecimal.valueOf(60), policy.getReorderQty());
    }

    @Test
    void getById_Success() {
        when(policies.findById(policyId)).thenReturn(Optional.of(policy));
        when(itemService.getById(itemId)).thenReturn(item);

        ReorderPolicyResponse res = service.getById(policyId);

        assertNotNull(res);
        assertEquals(policyId, res.id());
    }

    @Test
    void search_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReorderPolicy> page = new PageImpl<>(List.of(policy), pageable, 1);

        when(policies.findByWarehouseIdAndActive(eq(warehouseId), eq(true), any(Pageable.class))).thenReturn(page);
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));

        PageResponse<ReorderPolicyResponse> res = service.search(warehouseId, true, pageable);

        assertNotNull(res);
        assertEquals(1, res.totalElements());
    }

    @Test
    void findBelowMinimum_Success() {
        when(policies.findByWarehouseIdAndActiveTrue(warehouseId)).thenReturn(List.of(policy));
        when(queries.getAvailableInStorage(warehouseId, Set.of(itemId))).thenReturn(Map.of(itemId, BigDecimal.valueOf(5)));
        when(itemService.findAllByIds(Set.of(itemId))).thenReturn(List.of(item));

        List<LowStockResponse> res = service.findBelowMinimum(warehouseId);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(BigDecimal.valueOf(5), res.get(0).onHand());
    }
}

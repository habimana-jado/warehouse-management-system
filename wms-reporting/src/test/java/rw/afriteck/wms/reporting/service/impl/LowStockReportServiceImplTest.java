package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.service.ReorderPolicyService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LowStockReportServiceImplTest {

    @Mock
    private ReorderPolicyService reorderPolicies;

    @InjectMocks
    private LowStockReportServiceImpl service;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
    }

    @Test
    void listAndCsv() {
        UUID itemId = UUID.randomUUID();
        LowStockResponse lowStock = new LowStockResponse(
                itemId, "SKU1", "Item 1", "PCS",
                new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("20"),
                new BigDecimal("50"), new BigDecimal("45"));

        when(reorderPolicies.findBelowMinimum(warehouseId)).thenReturn(List.of(lowStock));

        List<LowStockResponse> list = service.list(warehouseId);
        assertEquals(1, list.size());
        assertEquals("SKU1", list.get(0).itemSku());

        byte[] csv = service.csv(warehouseId);
        assertNotNull(csv);
        String csvText = new String(csv, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("sku,name,uom,on_hand,min_qty,reorder_qty,max_qty,suggested_qty"));
        assertTrue(csvText.contains("SKU1,Item 1,PCS,5,10,20,50,45"));
    }
}

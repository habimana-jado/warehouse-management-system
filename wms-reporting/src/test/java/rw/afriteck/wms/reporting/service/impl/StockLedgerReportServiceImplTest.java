package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLedgerReportServiceImplTest {

    @Mock
    private InventoryQueryService inventory;
    @Mock
    private ReportingProperties props;

    @InjectMocks
    private StockLedgerReportServiceImpl service;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
    }

    @Test
    void exportCsv_missingWarehouseId_throwsException() {
        MovementFilter filter = new MovementFilter(null, null, null, null, null, null, null, null, null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.exportCsv(filter));
        assertEquals("WAREHOUSE_REQUIRED", ex.getCode());
    }

    @Test
    void exportCsv_exportTooLarge_throwsException() {
        MovementFilter filter = new MovementFilter(warehouseId, null, null, null, null, null, null, null, null, null);

        PageResponse<MovementHistoryResponse> page = new PageResponse<>(List.of(), 0, 100, 30000L, 300);
        when(inventory.history(eq(filter), any(PageRequest.class))).thenReturn(page);
        when(props.exportMaxRows()).thenReturn(20000);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.exportCsv(filter));
        assertEquals("EXPORT_TOO_LARGE", ex.getCode());
    }

    @Test
    void exportCsv_success() {
        MovementFilter filter = new MovementFilter(warehouseId, null, null, null, null, null, null, null, null, null);

        MovementHistoryResponse row = new MovementHistoryResponse(
                UUID.randomUUID(), UUID.randomUUID(), MovementType.RECEIPT, Instant.parse("2026-05-15T10:00:00Z"),
                warehouseId, UUID.randomUUID(), "BIN1",
                UUID.randomUUID(), "SKU1", "Item 1",
                null, "LOT1", null, null,
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.valueOf(100),
                "PO", UUID.randomUUID(), "PO-001", "Note");

        PageResponse<MovementHistoryResponse> page = new PageResponse<>(List.of(row), 0, 100, 1L, 1);
        when(inventory.history(eq(filter), any(PageRequest.class))).thenReturn(page);
        when(props.exportMaxRows()).thenReturn(20000);
        when(props.zoneId()).thenReturn(ZoneId.of("UTC"));

        byte[] csv = service.exportCsv(filter);
        assertNotNull(csv);
        String text = new String(csv, StandardCharsets.UTF_8);
        assertTrue(text.contains("occurred_at,type,location,sku,item,lot,expiry_date,serial,quantity,balance_after,unit_cost,reference_type,reference_number,note,transaction_id"));
        assertTrue(text.contains("SKU1,Item 1,LOT1"));
    }
}

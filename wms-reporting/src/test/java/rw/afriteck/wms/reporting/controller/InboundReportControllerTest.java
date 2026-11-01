package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.OpenPurchaseOrderRowResponse;
import rw.afriteck.wms.reporting.dtos.PutawayBacklogResponse;
import rw.afriteck.wms.reporting.dtos.SupplierPerformanceResponse;
import rw.afriteck.wms.reporting.service.InboundReportService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InboundReportControllerTest {

    @Mock
    private InboundReportService service;
    @Mock
    private UserContext userContext;

    private InboundReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new InboundReportController(service, userContext);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void openOrders_delegatesToServiceAndChecksUserContext() {
        PageResponse<OpenPurchaseOrderRowResponse> expected = new PageResponse<>(List.of(), 0, 50, 0L, 0);

        when(service.openOrders(warehouseId, true, 0, 50)).thenReturn(expected);

        var actual = controller.openOrders(warehouseId, true, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).openOrders(warehouseId, true, 0, 50);
    }

    @Test
    void supplierPerformance_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 1);
        SupplierPerformanceResponse expected = new SupplierPerformanceResponse(warehouseId, from, to, List.of());

        when(service.supplierPerformance(warehouseId, from, to)).thenReturn(expected);

        var actual = controller.supplierPerformance(warehouseId, from, to);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).supplierPerformance(warehouseId, from, to);
    }

    @Test
    void putawayBacklog_delegatesToServiceAndChecksUserContext() {
        PutawayBacklogResponse expected = new PutawayBacklogResponse(
                warehouseId, 0L, null, new PageResponse<>(List.of(), 0, 50, 0L, 0));

        when(service.putawayBacklog(warehouseId, 0, 50)).thenReturn(expected);

        var actual = controller.putawayBacklog(warehouseId, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).putawayBacklog(warehouseId, 0, 50);
    }
}

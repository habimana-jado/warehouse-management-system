package rw.afriteck.wms.reporting.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.reporting.dtos.OpenDemandRowResponse;
import rw.afriteck.wms.reporting.dtos.OutboundFulfillmentResponse;
import rw.afriteck.wms.reporting.dtos.PickingReportResponse;
import rw.afriteck.wms.reporting.dtos.ShipmentsReportResponse;
import rw.afriteck.wms.reporting.service.OutboundReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboundReportControllerTest {

    @Mock
    private OutboundReportService service;
    @Mock
    private UserContext userContext;

    private OutboundReportController controller;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        controller = new OutboundReportController(service, userContext);
        warehouseId = UUID.randomUUID();
    }

    @Test
    void fulfillment_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        OutboundFulfillmentResponse expected = new OutboundFulfillmentResponse(
                warehouseId, from, to, Map.of(), 10L, 8L, BigDecimal.TEN, BigDecimal.TEN, 5L, 4L, BigDecimal.ONE, 0L);

        when(service.fulfillment(warehouseId, from, to)).thenReturn(expected);

        var actual = controller.fulfillment(warehouseId, from, to);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).fulfillment(warehouseId, from, to);
    }

    @Test
    void picking_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        PickingReportResponse expected = new PickingReportResponse(
                warehouseId, from, to, 5L, 100L, 20L, 2L, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO);

        when(service.picking(warehouseId, from, to)).thenReturn(expected);

        var actual = controller.picking(warehouseId, from, to);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).picking(warehouseId, from, to);
    }

    @Test
    void shipments_delegatesToServiceAndChecksUserContext() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        ShipmentsReportResponse expected = new ShipmentsReportResponse(
                warehouseId, from, to, 10L, 15L, 8L, List.of());

        when(service.shipments(warehouseId, from, to)).thenReturn(expected);

        var actual = controller.shipments(warehouseId, from, to);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).shipments(warehouseId, from, to);
    }

    @Test
    void openDemand_delegatesToServiceAndChecksUserContext() {
        PageResponse<OpenDemandRowResponse> expected = new PageResponse<>(List.of(), 0, 50, 0L, 0);

        when(service.openDemand(warehouseId, true, 0, 50)).thenReturn(expected);

        var actual = controller.openDemand(warehouseId, true, 0, 50);

        assertEquals(expected, actual);
        verify(userContext).requireWarehouseAccess(warehouseId);
        verify(service).openDemand(warehouseId, true, 0, 50);
    }
}

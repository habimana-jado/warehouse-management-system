package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.reporting.dtos.*;
import rw.afriteck.wms.reporting.repository.OutboundReportRepository;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboundReportServiceImplTest {

    @Mock
    private OutboundReportRepository repository;
    @Mock
    private InventoryQueryService inventory;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;
    @Mock
    private ReportingProperties props;

    @InjectMocks
    private OutboundReportServiceImpl service;

    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
    }

    @Test
    void fulfillment() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        when(clock.dayRange(from, to, 30)).thenReturn(new ReportClock.DayRange(from, to));
        when(props.zone()).thenReturn("UTC");
        when(clock.today()).thenReturn(to);

        OutboundReportRepository.LineAgg finished = new OutboundReportRepository.LineAgg(
                10L, 8L, new BigDecimal("100"), new BigDecimal("90"));
        OutboundReportRepository.OnTimeAgg onTime = new OutboundReportRepository.OnTimeAgg(8L, 7L);

        when(repository.finishedLines(warehouseId, from, to)).thenReturn(finished);
        when(repository.onTime(warehouseId, from, to, "UTC")).thenReturn(onTime);
        when(repository.ordersByStatus(warehouseId, from, to)).thenReturn(Map.of("SHIPPED", 8L));
        when(repository.overdueOpenOrders(warehouseId, to)).thenReturn(1L);

        OutboundFulfillmentResponse resp = service.fulfillment(warehouseId, from, to);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(10L, resp.finishedLines());
        assertEquals(8L, resp.fullyShippedLines());
        assertEquals(new BigDecimal("80.00"), resp.lineFillRate());
        assertEquals(new BigDecimal("90.00"), resp.qtyFillRate());
        assertEquals(1L, resp.overdueOpenOrders());
    }

    @Test
    void picking() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 15);
        when(clock.dayRange(from, to, 30)).thenReturn(new ReportClock.DayRange(from, to));

        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-05-15T00:00:00Z");
        when(clock.startOfDay(from)).thenReturn(start);
        when(clock.startOfDay(to)).thenReturn(end);

        OutboundReportRepository.PickLineAgg pickLines = new OutboundReportRepository.PickLineAgg(
                20L, 2L, new BigDecimal("200"), new BigDecimal("180"), new BigDecimal("20"));
        OutboundReportRepository.PickListAgg pickLists = new OutboundReportRepository.PickListAgg(5L, 300L);

        when(repository.pickLines(warehouseId, start, end)).thenReturn(pickLines);
        when(repository.pickLists(warehouseId, start, end)).thenReturn(pickLists);

        PickingReportResponse resp = service.picking(warehouseId, from, to);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(5L, resp.completedPickLists());
        assertEquals(20L, resp.lines());
        assertEquals(2L, resp.shortLines());
    }

    @Test
    void shipments_dateRangeTooLarge() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 1);
        when(clock.dayRange(from, to, 30)).thenReturn(new ReportClock.DayRange(from, to));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.shipments(warehouseId, from, to));
        assertEquals("DATE_RANGE_TOO_LARGE", ex.getCode());
    }

    @Test
    void openDemand_calculatesShortfallAndSorts() {
        UUID itemId1 = UUID.randomUUID();
        UUID itemId2 = UUID.randomUUID();

        OutboundReportRepository.DemandRow d1 = new OutboundReportRepository.DemandRow(
                itemId1, 3L, new BigDecimal("50"), LocalDate.of(2026, 6, 1));
        OutboundReportRepository.DemandRow d2 = new OutboundReportRepository.DemandRow(
                itemId2, 1L, new BigDecimal("20"), LocalDate.of(2026, 5, 20));

        when(repository.openDemand(warehouseId)).thenReturn(List.of(d1, d2));
        when(inventory.getAvailableInStorage(eq(warehouseId), any()))
                .thenReturn(Map.of(itemId1, new BigDecimal("10"), itemId2, new BigDecimal("30"))); // item1 shortfall=40, item2 shortfall=0

        ItemResponse item1 = new ItemResponse(itemId1, "SKU1", "Item 1", null, null, "Cat", "PCS", TrackingType.NONE, false, true);
        ItemResponse item2 = new ItemResponse(itemId2, "SKU2", "Item 2", null, null, "Cat", "PCS", TrackingType.NONE, false, true);

        when(lookups.itemsById(any())).thenReturn(Map.of(itemId1, item1, itemId2, item2));

        // When shortfallOnly=true, only item1 is returned
        PageResponse<OpenDemandRowResponse> pageShortfall = service.openDemand(warehouseId, true, 0, 50);
        assertEquals(1, pageShortfall.totalElements());
        assertEquals(itemId1, pageShortfall.content().get(0).itemId());
        assertEquals(new BigDecimal("40"), pageShortfall.content().get(0).shortfallQty());

        // When shortfallOnly=false, both returned, item1 first because shortfall is 40 vs 0
        PageResponse<OpenDemandRowResponse> pageAll = service.openDemand(warehouseId, false, 0, 50);
        assertEquals(2, pageAll.totalElements());
        assertEquals(itemId1, pageAll.content().get(0).itemId());
    }
}

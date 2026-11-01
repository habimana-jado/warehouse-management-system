package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.reporting.dtos.OpenPurchaseOrderRowResponse;
import rw.afriteck.wms.reporting.dtos.PutawayBacklogResponse;
import rw.afriteck.wms.reporting.dtos.SupplierPerformanceResponse;
import rw.afriteck.wms.reporting.repository.InboundReportRepository;
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
class InboundReportServiceImplTest {

    @Mock
    private InboundReportRepository repository;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;
    @Mock
    private ReportingProperties props;

    @InjectMocks
    private InboundReportServiceImpl service;

    private UUID warehouseId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        today = LocalDate.of(2026, 5, 15);
    }

    @Test
    void openOrders() {
        when(clock.today()).thenReturn(today);

        UUID poId = UUID.randomUUID();
        UUID supId = UUID.randomUUID();
        LocalDate expected = LocalDate.of(2026, 5, 10); // 5 days late

        InboundReportRepository.OpenOrderRow row = new InboundReportRepository.OpenOrderRow(
                poId, "PO-001", supId, "APPROVED", LocalDate.of(2026, 5, 1), expected, 5, 2);

        when(repository.countOpenOrders(warehouseId, true, today)).thenReturn(1L);
        when(repository.findOpenOrders(warehouseId, true, today, 50, 0L)).thenReturn(List.of(row));

        SupplierResponse sup = new SupplierResponse(supId, "SUP1", "Supplier 1", "Contact", "123", "a@b.com", "Addr", "City", "Country", true);
        when(lookups.suppliersById(any())).thenReturn(Map.of(supId, sup));

        PageResponse<OpenPurchaseOrderRowResponse> page = service.openOrders(warehouseId, true, 0, 50);

        assertEquals(1, page.totalElements());
        OpenPurchaseOrderRowResponse resp = page.content().get(0);
        assertEquals("PO-001", resp.poNumber());
        assertEquals("Supplier 1", resp.supplierName());
        assertEquals(5L, resp.daysLate());
    }

    @Test
    void supplierPerformance() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 1);
        when(clock.dayRange(from, to, 90)).thenReturn(new ReportClock.DayRange(from, to));
        when(props.zone()).thenReturn("UTC");

        UUID supId = UUID.randomUUID();
        InboundReportRepository.SupplierAggRow aggRow = new InboundReportRepository.SupplierAggRow(
                supId, 10L, 20L, 18L, new BigDecimal("100"), new BigDecimal("90"),
                new BigDecimal("90"), BigDecimal.ZERO, 10L, 9L);

        when(repository.supplierAggregates(warehouseId, from, to, "UTC")).thenReturn(List.of(aggRow));

        SupplierResponse sup = new SupplierResponse(supId, "SUP1", "Supplier 1", "Contact", "123", "a@b.com", "Addr", "City", "Country", true);
        when(lookups.suppliersById(any())).thenReturn(Map.of(supId, sup));

        SupplierPerformanceResponse resp = service.supplierPerformance(warehouseId, from, to);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(1, resp.rows().size());
        assertEquals("Supplier 1", resp.rows().get(0).supplierName());
        assertEquals(new BigDecimal("90.00"), resp.rows().get(0).lineFillRate());
        assertEquals(new BigDecimal("90.00"), resp.rows().get(0).onTimeRate());
    }

    @Test
    void putawayBacklog() {
        InboundReportRepository.BacklogSummary summary = new InboundReportRepository.BacklogSummary(1, Instant.now());
        UUID receiptId = UUID.randomUUID();
        UUID lineId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        InboundReportRepository.BacklogRow row = new InboundReportRepository.BacklogRow(
                lineId, receiptId, "GR-001", Instant.now().minusSeconds(7200), itemId, BigDecimal.TEN, null);

        when(repository.backlogSummary(warehouseId)).thenReturn(summary);
        when(repository.backlogRows(warehouseId, 50, 0L)).thenReturn(List.of(row));
        when(clock.now()).thenReturn(Instant.now());

        PutawayBacklogResponse resp = service.putawayBacklog(warehouseId, 0, 50);

        assertEquals(warehouseId, resp.warehouseId());
        assertEquals(1, resp.pendingLines());
        assertEquals(1, resp.rows().content().size());
    }
}

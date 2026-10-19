package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.reporting.dtos.ValuationReportResponse;
import rw.afriteck.wms.reporting.repository.StockReportRepository;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValuationReportServiceImplTest {

    @Mock
    private StockReportRepository repository;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;
    @Mock
    private ReportingProperties props;

    @InjectMocks
    private ValuationReportServiceImpl service;

    private UUID warehouseId;
    private Instant now;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        now = Instant.now();
    }

    @Test
    void valuation_calculatesValuedAndUnknowns() {
        when(props.currency()).thenReturn("RWF");
        when(clock.now()).thenReturn(now);

        UUID itemValuedId = UUID.randomUUID();
        UUID itemUnknownId = UUID.randomUUID();

        StockReportRepository.CostedOnHandRow row1 = new StockReportRepository.CostedOnHandRow(
                itemValuedId, new BigDecimal("10"), new BigDecimal("250.00"), now);
        StockReportRepository.CostedOnHandRow row2 = new StockReportRepository.CostedOnHandRow(
                itemUnknownId, new BigDecimal("5"), null, null);

        when(repository.onHandWithLastCost(warehouseId)).thenReturn(List.of(row1, row2));

        ItemResponse item1 = new ItemResponse(itemValuedId, "SKU1", "Item Valued", null, null, "Cat", "PCS", TrackingType.NONE, false, true);
        ItemResponse item2 = new ItemResponse(itemUnknownId, "SKU2", "Item Unknown", null, null, "Cat", "PCS", TrackingType.NONE, false, true);

        when(lookups.itemsById(any())).thenReturn(Map.of(itemValuedId, item1, itemUnknownId, item2));

        ValuationReportResponse response = service.valuation(warehouseId, null, 0, 50);

        assertEquals(warehouseId, response.warehouseId());
        assertEquals("RWF", response.currency());
        assertEquals(now, response.asOf());
        assertEquals(new BigDecimal("2500.0000"), response.totalValue());
        assertEquals(1, response.itemsValued());
        assertEquals(1, response.itemsWithoutCost());
        assertEquals(new BigDecimal("5"), response.unknownCostQty());

        assertEquals(2, response.rows().totalElements());
        // Row 1 (value 2500) comes first
        assertEquals(itemValuedId, response.rows().content().get(0).itemId());
        assertEquals(new BigDecimal("2500.0000"), response.rows().content().get(0).value());
    }
}

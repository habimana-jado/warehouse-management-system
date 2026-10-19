package rw.afriteck.wms.reporting.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.reporting.dtos.StockSummaryResponse;
import rw.afriteck.wms.reporting.repository.StockReportRepository;
import rw.afriteck.wms.reporting.support.ReportLookups;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockSummaryServiceImplTest {

    @Mock
    private StockReportRepository repository;
    @Mock
    private ReportLookups lookups;

    @InjectMocks
    private StockSummaryServiceImpl service;

    private UUID warehouseId;
    private UUID item1Id;
    private UUID item2Id;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        item1Id = UUID.randomUUID();
        item2Id = UUID.randomUUID();
    }

    @Test
    void summary_filtersAndSortsBySku() {
        Set<UUID> storageBins = Set.of(UUID.randomUUID());
        Set<UUID> receivingBins = Set.of(UUID.randomUUID());
        Set<UUID> damagedBins = Set.of(UUID.randomUUID());

        when(lookups.binIds(warehouseId, LocationUsage.STORAGE)).thenReturn(storageBins);
        when(lookups.binIds(warehouseId, LocationUsage.RECEIVING)).thenReturn(receivingBins);
        when(lookups.binIds(warehouseId, LocationUsage.DAMAGED)).thenReturn(damagedBins);

        StockReportRepository.OnHandRow row1 = new StockReportRepository.OnHandRow(
                item1Id, new BigDecimal("100"), new BigDecimal("60"), new BigDecimal("20"), new BigDecimal("10"));
        StockReportRepository.OnHandRow row2 = new StockReportRepository.OnHandRow(
                item2Id, new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("5"));

        when(repository.onHandByItem(warehouseId, storageBins, receivingBins, damagedBins))
                .thenReturn(List.of(row1, row2));

        ItemResponse item1 = new ItemResponse(item1Id, "SKU-B", "Banana", "BAR1", null, "Fruit", "PCS", TrackingType.NONE, false, true);
        ItemResponse item2 = new ItemResponse(item2Id, "SKU-A", "Apple", "BAR2", null, "Fruit", "PCS", TrackingType.NONE, false, true);

        when(lookups.itemsById(any())).thenReturn(Map.of(item1Id, item1, item2Id, item2));

        PageResponse<StockSummaryResponse> page = service.summary(warehouseId, null, 0, 50);

        assertEquals(2, page.totalElements());
        assertEquals(2, page.content().size());
        // Row 2 (SKU-A) comes before Row 1 (SKU-B)
        assertEquals("SKU-A", page.content().get(0).itemSku());
        assertEquals("SKU-B", page.content().get(1).itemSku());

        // Check 'other' calculation for item1: 100 - 60 - 20 - 10 = 10
        assertEquals(new BigDecimal("10"), page.content().get(1).otherQty());
    }

    @Test
    void summary_withSearchQuery() {
        Set<UUID> storageBins = Set.of();
        Set<UUID> receivingBins = Set.of();
        Set<UUID> damagedBins = Set.of();

        when(lookups.binIds(any(), any())).thenReturn(storageBins);

        StockReportRepository.OnHandRow row1 = new StockReportRepository.OnHandRow(
                item1Id, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(repository.onHandByItem(any(), any(), any(), any())).thenReturn(List.of(row1));

        ItemResponse item1 = new ItemResponse(item1Id, "SKU-B", "Banana", "BAR1", null, "Fruit", "PCS", TrackingType.NONE, false, true);
        when(lookups.itemsById(any())).thenReturn(Map.of(item1Id, item1));

        PageResponse<StockSummaryResponse> page1 = service.summary(warehouseId, "banana", 0, 50);
        assertEquals(1, page1.totalElements());

        PageResponse<StockSummaryResponse> page2 = service.summary(warehouseId, "orange", 0, 50);
        assertEquals(0, page2.totalElements());
    }
}

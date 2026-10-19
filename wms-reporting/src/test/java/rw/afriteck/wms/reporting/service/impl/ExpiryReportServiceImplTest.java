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
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.reporting.dtos.ExpiryRowResponse;
import rw.afriteck.wms.reporting.enums.ExpiryStatus;
import rw.afriteck.wms.reporting.repository.StockReportRepository;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiryReportServiceImplTest {

    @Mock
    private StockReportRepository repository;
    @Mock
    private ReportLookups lookups;
    @Mock
    private ReportClock clock;
    @Mock
    private ReportingProperties props;

    @InjectMocks
    private ExpiryReportServiceImpl service;

    private UUID warehouseId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        warehouseId = UUID.randomUUID();
        today = LocalDate.of(2026, 5, 15);
    }

    @Test
    void expiry_invalidDays_throwsException() {
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> service.expiry(warehouseId, -1, true, 0, 50));
        assertEquals("DAYS_INVALID", ex1.getCode());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> service.expiry(warehouseId, 4000, true, 0, 50));
        assertEquals("DAYS_INVALID", ex2.getCode());
    }

    @Test
    void expiry_successExpiringAndExpired() {
        when(props.defaultExpiryDays()).thenReturn(30);
        when(clock.today()).thenReturn(today);

        UUID itemId = UUID.randomUUID();
        UUID locId = UUID.randomUUID();
        UUID lot1Id = UUID.randomUUID();
        UUID lot2Id = UUID.randomUUID();

        StockReportRepository.ExpiryRow rowExpired = new StockReportRepository.ExpiryRow(
                itemId, lot1Id, "LOT-01", LocalDate.of(2026, 5, 10), locId, BigDecimal.TEN);
        StockReportRepository.ExpiryRow rowExpiring = new StockReportRepository.ExpiryRow(
                itemId, lot2Id, "LOT-02", LocalDate.of(2026, 5, 20), locId, BigDecimal.valueOf(20));

        when(repository.countExpiring(eq(warehouseId), any(), eq(today), eq(true))).thenReturn(2L);
        when(repository.findExpiring(eq(warehouseId), any(), eq(today), eq(true), eq(50), eq(0L)))
                .thenReturn(List.of(rowExpired, rowExpiring));

        ItemResponse item = new ItemResponse(itemId, "SKU1", "Item 1", null, null, "Cat", "PCS", TrackingType.LOT, true, true);
        LocationResponse loc = new LocationResponse(locId, warehouseId, null, "Bin 1", "BIN1", LocationType.BIN, LocationUsage.STORAGE, null, null, true);

        when(lookups.itemsById(any())).thenReturn(Map.of(itemId, item));
        when(lookups.locationsById(any())).thenReturn(Map.of(locId, loc));

        PageResponse<ExpiryRowResponse> page = service.expiry(warehouseId, null, true, 0, 50);

        assertEquals(2, page.totalElements());
        ExpiryRowResponse expiredResp = page.content().get(0);
        assertEquals(ExpiryStatus.EXPIRED, expiredResp.status());
        assertEquals(-5, expiredResp.daysToExpiry());

        ExpiryRowResponse expiringResp = page.content().get(1);
        assertEquals(ExpiryStatus.EXPIRING, expiringResp.status());
        assertEquals(5, expiringResp.daysToExpiry());
    }
}

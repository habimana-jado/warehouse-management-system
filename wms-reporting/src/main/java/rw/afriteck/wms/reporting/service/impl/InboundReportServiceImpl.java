package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.reporting.dtos.*;
import rw.afriteck.wms.reporting.repository.InboundReportRepository;
import rw.afriteck.wms.reporting.service.InboundReportService;
import rw.afriteck.wms.reporting.support.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundReportServiceImpl implements InboundReportService {

    private static final int DEFAULT_PERFORMANCE_DAYS = 90;

    private final InboundReportRepository repository;
    private final ReportLookups lookups;
    private final ReportClock clock;
    private final ReportingProperties props;

    @Override
    public PageResponse<OpenPurchaseOrderRowResponse> openOrders(UUID warehouseId, boolean overdueOnly,
                                                                 int page, int size) {
        LocalDate today = clock.today();
        int s = Paging.size(size), p = Paging.page(page);
        long total = repository.countOpenOrders(warehouseId, overdueOnly, today);
        var rows = repository.findOpenOrders(warehouseId, overdueOnly, today, s, (long) p * s);
        Map<UUID, SupplierResponse> suppliers = lookups.suppliersById(
                rows.stream().map(InboundReportRepository.OpenOrderRow::supplierId).toList());

        List<OpenPurchaseOrderRowResponse> content = rows.stream().map(r -> {
            SupplierResponse sup = suppliers.get(r.supplierId());
            Long late = r.expectedDate() != null && r.expectedDate().isBefore(today)
                    ? ChronoUnit.DAYS.between(r.expectedDate(), today) : null;
            return new OpenPurchaseOrderRowResponse(r.id(), r.poNumber(), r.supplierId(),
                    sup == null ? null : sup.name(), r.status(), r.orderDate(), r.expectedDate(), late,
                    r.lines(), r.fulfilledLines());
        }).toList();
        return new PageResponse<>(content, p, s, total, Paging.totalPages(total, s));
    }

    @Override
    public SupplierPerformanceResponse supplierPerformance(UUID warehouseId, LocalDate from, LocalDate to) {
        ReportClock.DayRange range = clock.dayRange(from, to, DEFAULT_PERFORMANCE_DAYS);
        var aggs = repository.supplierAggregates(warehouseId, range.from(), range.to(), props.zone());
        Map<UUID, SupplierResponse> suppliers = lookups.suppliersById(
                aggs.stream().map(InboundReportRepository.SupplierAggRow::supplierId).toList());

        List<SupplierPerformanceRowResponse> rows = aggs.stream().map(a -> {
            SupplierResponse sup = suppliers.get(a.supplierId());
            BigDecimal received = a.acceptedQty().add(a.rejectedQty());
            return new SupplierPerformanceRowResponse(a.supplierId(),
                    sup == null ? null : sup.code(), sup == null ? null : sup.name(),
                    a.purchaseOrders(), a.lines(), a.linesFulfilled(),
                    Rates.percent(a.linesFulfilled(), a.lines()),
                    Rates.percent(a.fulfilledQty(), a.orderedQty()),
                    Rates.percent(a.rejectedQty(), received),
                    a.datedReceived(), a.onTime(), Rates.percent(a.onTime(), a.datedReceived()));
        }).sorted(Comparator.comparing(SupplierPerformanceRowResponse::supplierName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList();
        return new SupplierPerformanceResponse(warehouseId, range.from(), range.to(), rows);
    }

    @Override
    public PutawayBacklogResponse putawayBacklog(UUID warehouseId, int page, int size) {
        int s = Paging.size(size), p = Paging.page(page);
        var summary = repository.backlogSummary(warehouseId);
        var rows = repository.backlogRows(warehouseId, s, (long) p * s);
        Map<UUID, ItemResponse> items = lookups.itemsById(
                rows.stream().map(InboundReportRepository.BacklogRow::itemId).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locations = lookups.locationsById(rows.stream()
                .map(InboundReportRepository.BacklogRow::suggestedLocationId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Instant now = clock.now();

        List<PutawayBacklogRowResponse> content = rows.stream().map(r -> {
            ItemResponse item = items.get(r.itemId());
            LocationResponse loc = r.suggestedLocationId() == null ? null : locations.get(r.suggestedLocationId());
            return new PutawayBacklogRowResponse(r.receiptId(), r.receiptNumber(), r.receivedAt(),
                    Duration.between(r.receivedAt(), now).toHours(), r.lineId(), r.itemId(),
                    item == null ? null : item.sku(), item == null ? null : item.name(), r.acceptedQty(),
                    r.suggestedLocationId(), loc == null ? null : loc.code());
        }).toList();
        return new PutawayBacklogResponse(warehouseId, summary.pendingLines(), summary.oldestReceivedAt(),
                new PageResponse<>(content, p, s, summary.pendingLines(),
                        Paging.totalPages(summary.pendingLines(), s)));
    }
}

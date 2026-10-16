package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.reporting.dtos.*;
import rw.afriteck.wms.reporting.repository.AccuracyReportRepository;
import rw.afriteck.wms.reporting.service.AccuracyReportService;
import rw.afriteck.wms.reporting.support.Rates;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccuracyReportServiceImpl implements AccuracyReportService {

    private final AccuracyReportRepository repository;
    private final ReportLookups lookups;
    private final ReportClock clock;

    @Override
    public CountAccuracyResponse countAccuracy(UUID warehouseId, LocalDate from, LocalDate to, int topItems) {
        ReportClock.DayRange r = clock.dayRange(from, to, 90);
        Instant start = clock.startOfDay(r.from()), end = clock.startOfDay(r.to());
        var rows = repository.countRows(warehouseId, start, end);
        var top = repository.topVarianceItems(warehouseId, start, end, Math.min(Math.max(topItems, 1), 50));

        Map<UUID, LocationResponse> zones = lookups.locationsById(rows.stream()
                .map(AccuracyReportRepository.CountRow::zoneLocationId).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<UUID, String> approvers = lookups.workerNames(rows.stream()
                .map(AccuracyReportRepository.CountRow::approvedBy).toList());
        Map<UUID, ItemResponse> items = lookups.itemsById(top.stream()
                .map(AccuracyReportRepository.VarianceRow::itemId).toList());

        long lines = 0, matching = 0;
        BigDecimal net = BigDecimal.ZERO, abs = BigDecimal.ZERO;
        List<CountAccuracyRowResponse> countList = new ArrayList<>(rows.size());
        for (var c : rows) {
            lines += c.lines(); matching += c.matching();
            net = net.add(c.net()); abs = abs.add(c.abs());
            LocationResponse zone = c.zoneLocationId() == null ? null : zones.get(c.zoneLocationId());
            countList.add(new CountAccuracyRowResponse(c.countId(), c.countNumber(), c.zoneLocationId(),
                    zone == null ? null : zone.code(), c.completedAt(), c.approvedBy(),
                    c.approvedBy() == null ? null : approvers.get(c.approvedBy()),
                    c.lines(), c.matching(), c.over(), c.under(), c.net(), c.abs(),
                    Rates.percent(c.matching(), c.lines())));
        }
        List<VarianceItemResponse> topItemRows = top.stream().map(v -> {
            ItemResponse item = items.get(v.itemId());
            return new VarianceItemResponse(v.itemId(), item == null ? null : item.sku(),
                    item == null ? null : item.name(), item == null ? null : item.baseUom(),
                    v.lines(), v.net(), v.abs());
        }).toList();

        return new CountAccuracyResponse(warehouseId, r.from(), r.to(), rows.size(), lines, matching,
                Rates.percent(matching, lines), net, abs, countList, topItemRows);
    }

    @Override
    public AdjustmentsReportResponse adjustments(UUID warehouseId, LocalDate from, LocalDate to) {
        ReportClock.DayRange r = clock.dayRange(from, to, 30);
        var byReason = repository.adjustmentsByReason(warehouseId, clock.startOfDay(r.from()), clock.startOfDay(r.to()));
        var pending = repository.pendingAdjustments(warehouseId);
        List<AdjustmentReasonRowResponse> rows = byReason.stream()
                .map(a -> new AdjustmentReasonRowResponse(a.code(), a.name(), a.adjustments(), a.lines(),
                        a.increased(), a.decreased())).toList();
        return new AdjustmentsReportResponse(warehouseId, r.from(), r.to(),
                rows.stream().mapToLong(AdjustmentReasonRowResponse::adjustments).sum(),
                pending.count(), pending.oldest(), rows);
    }
}

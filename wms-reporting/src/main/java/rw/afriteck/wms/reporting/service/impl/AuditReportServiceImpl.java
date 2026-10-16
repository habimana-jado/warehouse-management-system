package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.reporting.dtos.*;
import rw.afriteck.wms.reporting.enums.AuditSource;
import rw.afriteck.wms.reporting.repository.AuditReportRepository;
import rw.afriteck.wms.reporting.service.AuditReportService;
import rw.afriteck.wms.reporting.support.Paging;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditReportServiceImpl implements AuditReportService {

    private static final int MAX_DAYS = 31;

    private final AuditReportRepository repository;
    private final ReportLookups lookups;
    private final ReportClock clock;

    @Override
    public AuditFeedResponse feed(UUID warehouseId, LocalDate from, LocalDate to, Set<AuditSource> sources,
                                  UUID actorId, int page, int size) {
        ReportClock.DayRange r = clock.dayRange(from, to, 7);
        if (ChronoUnit.DAYS.between(r.from(), r.to()) > MAX_DAYS) {
            throw new BusinessException("DATE_RANGE_TOO_LARGE", "The audit log is limited to " + MAX_DAYS + " days per request");
        }
        Set<AuditSource> use = sources == null || sources.isEmpty()
                ? EnumSet.allOf(AuditSource.class) : EnumSet.copyOf(sources);
        int s = Paging.size(size), p = Paging.page(page);

        // fetch one extra row to know whether another page exists (no count query over a month of ledger rows)
        var rows = repository.search(warehouseId, clock.startOfDay(r.from()), clock.startOfDay(r.to()),
                use, actorId, s + 1, (long) p * s);
        boolean hasMore = rows.size() > s;
        var pageRows = hasMore ? rows.subList(0, s) : rows;

        Map<UUID, String> actors = lookups.workerNames(pageRows.stream()
                .map(AuditReportRepository.AuditRow::actorId).toList());
        Map<UUID, ItemResponse> items = lookups.itemsById(pageRows.stream()
                .map(AuditReportRepository.AuditRow::itemId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locations = lookups.locationsById(pageRows.stream()
                .map(AuditReportRepository.AuditRow::locationId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<AuditEventResponse> content = pageRows.stream().map(a -> {
            ItemResponse item = a.itemId() == null ? null : items.get(a.itemId());
            LocationResponse loc = a.locationId() == null ? null : locations.get(a.locationId());
            return new AuditEventResponse(a.occurredAt(), AuditSource.valueOf(a.source()), a.event(),
                    a.actorId(), a.actorId() == null ? null : actors.get(a.actorId()),
                    a.itemId(), item == null ? null : item.sku(), item == null ? null : item.name(),
                    a.locationId(), loc == null ? null : loc.code(), a.quantity(),
                    a.referenceType(), a.referenceId(), a.referenceNumber(), a.detail());
        }).toList();
        return new AuditFeedResponse(content, p, s, hasMore);
    }
}

package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.reporting.dtos.ExpiryRowResponse;
import rw.afriteck.wms.reporting.enums.ExpiryStatus;
import rw.afriteck.wms.reporting.repository.StockReportRepository;
import rw.afriteck.wms.reporting.service.ExpiryReportService;
import rw.afriteck.wms.reporting.support.Paging;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpiryReportServiceImpl implements ExpiryReportService {

    private final StockReportRepository repository;
    private final ReportLookups lookups;
    private final ReportClock clock;
    private final ReportingProperties props;

    @Override
    public PageResponse<ExpiryRowResponse> expiry(UUID warehouseId, Integer days, boolean includeExpired,
                                                  int page, int size) {
        int horizon = days == null ? props.defaultExpiryDays() : days;
        if (horizon < 0 || horizon > 3650) {
            throw new BusinessException("DAYS_INVALID", "days must be between 0 and 3650");
        }
        LocalDate today = clock.today();
        LocalDate cutoff = today.plusDays(horizon);
        int s = Paging.size(size), p = Paging.page(page);

        long total = repository.countExpiring(warehouseId, cutoff, today, includeExpired);
        List<StockReportRepository.ExpiryRow> rows =
                repository.findExpiring(warehouseId, cutoff, today, includeExpired, s, (long) p * s);

        Map<UUID, ItemResponse> itemMap = lookups.itemsById(
                rows.stream().map(StockReportRepository.ExpiryRow::itemId).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locMap = lookups.locationsById(
                rows.stream().map(StockReportRepository.ExpiryRow::locationId).collect(Collectors.toSet()));

        List<ExpiryRowResponse> content = rows.stream().map(r -> {
            ItemResponse item = itemMap.get(r.itemId());
            LocationResponse loc = locMap.get(r.locationId());
            return new ExpiryRowResponse(r.itemId(),
                    item == null ? null : item.sku(), item == null ? null : item.name(),
                    item == null ? null : item.baseUom(),
                    r.lotId(), r.lotNumber(), r.expiryDate(),
                    ChronoUnit.DAYS.between(today, r.expiryDate()),
                    r.expiryDate().isBefore(today) ? ExpiryStatus.EXPIRED : ExpiryStatus.EXPIRING,
                    r.locationId(), loc == null ? null : loc.code(), loc == null ? null : loc.usage(),
                    r.quantity());
        }).toList();
        return new PageResponse<>(content, p, s, total, Paging.totalPages(total, s));
    }
}

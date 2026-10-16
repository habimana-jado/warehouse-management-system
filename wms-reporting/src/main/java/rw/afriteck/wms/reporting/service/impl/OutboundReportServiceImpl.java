package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.reporting.dtos.*;
import rw.afriteck.wms.reporting.repository.OutboundReportRepository;
import rw.afriteck.wms.reporting.service.OutboundReportService;
import rw.afriteck.wms.reporting.support.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundReportServiceImpl implements OutboundReportService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAILY_SERIES_DAYS = 366;

    private final OutboundReportRepository repository;
    private final InventoryQueryService inventory;
    private final ReportLookups lookups;
    private final ReportClock clock;
    private final ReportingProperties props;

    @Override
    public OutboundFulfillmentResponse fulfillment(UUID warehouseId, LocalDate from, LocalDate to) {
        ReportClock.DayRange r = clock.dayRange(from, to, DEFAULT_DAYS);
        var lines = repository.finishedLines(warehouseId, r.from(), r.to());
        var onTime = repository.onTime(warehouseId, r.from(), r.to(), props.zone());
        return new OutboundFulfillmentResponse(warehouseId, r.from(), r.to(),
                repository.ordersByStatus(warehouseId, r.from(), r.to()),
                lines.lines(), lines.fullyShipped(),
                Rates.percent(lines.fullyShipped(), lines.lines()),
                Rates.percent(lines.shippedQty(), lines.orderedQty()),
                onTime.dated(), onTime.onTime(), Rates.percent(onTime.onTime(), onTime.dated()),
                repository.overdueOpenOrders(warehouseId, clock.today()));
    }

    @Override
    public PickingReportResponse picking(UUID warehouseId, LocalDate from, LocalDate to) {
        ReportClock.DayRange r = clock.dayRange(from, to, DEFAULT_DAYS);
        Instant start = clock.startOfDay(r.from()), end = clock.startOfDay(r.to());
        var lines = repository.pickLines(warehouseId, start, end);
        var lists = repository.pickLists(warehouseId, start, end);
        return new PickingReportResponse(warehouseId, r.from(), r.to(), lists.lists(), lists.avgCycleSeconds(),
                lines.lines(), lines.shortLines(), Rates.percent(lines.shortLines(), lines.lines()),
                lines.plannedQty(), lines.pickedQty(), lines.shortQty());
    }

    @Override
    public ShipmentsReportResponse shipments(UUID warehouseId, LocalDate from, LocalDate to) {
        ReportClock.DayRange r = clock.dayRange(from, to, DEFAULT_DAYS);
        if (ChronoUnit.DAYS.between(r.from(), r.to()) > MAX_DAILY_SERIES_DAYS) {
            throw new BusinessException("DATE_RANGE_TOO_LARGE",
                    "The daily shipment series is limited to " + MAX_DAILY_SERIES_DAYS + " days");
        }
        var days = repository.shipmentsPerDay(warehouseId, clock.startOfDay(r.from()), clock.startOfDay(r.to()),
                props.zone());
        List<ShipmentDayResponse> series = days.stream()
                .map(d -> new ShipmentDayResponse(d.day(), d.shipments(), d.packages(), d.delivered())).toList();
        return new ShipmentsReportResponse(warehouseId, r.from(), r.to(),
                series.stream().mapToLong(ShipmentDayResponse::shipments).sum(),
                series.stream().mapToLong(ShipmentDayResponse::packages).sum(),
                series.stream().mapToLong(ShipmentDayResponse::delivered).sum(), series);
    }

    @Override
    public PageResponse<OpenDemandRowResponse> openDemand(UUID warehouseId, boolean shortfallOnly,
                                                          int page, int size) {
        var demand = repository.openDemand(warehouseId);
        List<UUID> itemIds = demand.stream().map(OutboundReportRepository.DemandRow::itemId).toList();
        Map<UUID, BigDecimal> available = itemIds.isEmpty() ? Map.of()
                : inventory.getAvailableInStorage(warehouseId, itemIds);
        Map<UUID, ItemResponse> items = lookups.itemsById(itemIds);

        List<OpenDemandRowResponse> rows = demand.stream().map(d -> {
                    BigDecimal avail = available.getOrDefault(d.itemId(), BigDecimal.ZERO);
                    BigDecimal shortfall = d.toPickQty().subtract(avail).max(BigDecimal.ZERO);
                    ItemResponse item = items.get(d.itemId());
                    return new OpenDemandRowResponse(d.itemId(),
                            item == null ? null : item.sku(), item == null ? null : item.name(),
                            item == null ? null : item.baseUom(),
                            d.orderLines(), d.toPickQty(), avail, shortfall, d.earliestRequired());
                }).filter(r -> !shortfallOnly || r.shortfallQty().signum() > 0)
                .sorted(Comparator.comparing(OpenDemandRowResponse::shortfallQty, Comparator.reverseOrder())
                        .thenComparing(OpenDemandRowResponse::earliestRequiredDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(OpenDemandRowResponse::itemSku,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return Paging.slice(rows, page, size);
    }
}
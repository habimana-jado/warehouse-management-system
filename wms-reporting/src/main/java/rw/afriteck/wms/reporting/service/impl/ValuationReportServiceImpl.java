package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.reporting.dtos.ValuationReportResponse;
import rw.afriteck.wms.reporting.dtos.ValuationRowResponse;
import rw.afriteck.wms.reporting.repository.StockReportRepository;
import rw.afriteck.wms.reporting.service.ValuationReportService;
import rw.afriteck.wms.reporting.support.Paging;
import rw.afriteck.wms.reporting.support.ReportClock;
import rw.afriteck.wms.reporting.support.ReportLookups;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValuationReportServiceImpl implements ValuationReportService {

    private final StockReportRepository repository;
    private final ReportLookups lookups;
    private final ReportClock clock;
    private final ReportingProperties props;

    @Override
    public ValuationReportResponse valuation(UUID warehouseId, String q, int page, int size) {
        List<StockReportRepository.CostedOnHandRow> rows = repository.onHandWithLastCost(warehouseId);
        Map<UUID, ItemResponse> itemMap = lookups.itemsById(
                rows.stream().map(StockReportRepository.CostedOnHandRow::itemId).toList());

        BigDecimal total = BigDecimal.ZERO, unknownQty = BigDecimal.ZERO;
        int valued = 0, unknown = 0;
        List<ValuationRowResponse> all = new ArrayList<>(rows.size());
        for (var r : rows) {
            BigDecimal value = r.lastUnitCost() == null ? null
                    : r.onHand().multiply(r.lastUnitCost()).setScale(4, RoundingMode.HALF_UP);
            if (value == null) { unknown++; unknownQty = unknownQty.add(r.onHand()); }
            else { valued++; total = total.add(value); }
            ItemResponse item = itemMap.get(r.itemId());
            all.add(new ValuationRowResponse(r.itemId(),
                    item == null ? null : item.sku(), item == null ? null : item.name(),
                    item == null ? null : item.baseUom(),
                    r.onHand(), r.lastUnitCost(), r.lastCostAt(), value));
        }

        // Totals cover the whole warehouse; the search text only narrows the rows shown.
        String needle = ReportLookups.needle(q);
        List<ValuationRowResponse> shown = all.stream()
                .filter(v -> needle == null || has(v.itemSku(), needle) || has(v.itemName(), needle))
                .sorted(Comparator.comparing(ValuationRowResponse::value, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ValuationRowResponse::itemSku, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return new ValuationReportResponse(warehouseId, props.currency(), clock.now(),
                total, valued, unknown, unknownQty, Paging.slice(shown, page, size));
    }

    private static boolean has(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
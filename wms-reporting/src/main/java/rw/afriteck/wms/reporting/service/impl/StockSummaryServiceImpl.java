package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.reporting.dtos.StockSummaryResponse;
import rw.afriteck.wms.reporting.repository.StockReportRepository;
import rw.afriteck.wms.reporting.service.StockSummaryService;
import rw.afriteck.wms.reporting.support.Paging;
import rw.afriteck.wms.reporting.support.ReportLookups;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockSummaryServiceImpl implements StockSummaryService {

    private final StockReportRepository repository;
    private final ReportLookups lookups;

    @Override
    public PageResponse<StockSummaryResponse> summary(UUID warehouseId, String q, int page, int size) {
        List<StockReportRepository.OnHandRow> rows = repository.onHandByItem(warehouseId,
                lookups.binIds(warehouseId, LocationUsage.STORAGE),
                lookups.binIds(warehouseId, LocationUsage.RECEIVING),
                lookups.binIds(warehouseId, LocationUsage.DAMAGED));
        Map<UUID, ItemResponse> itemMap = lookups.itemsById(
                rows.stream().map(StockReportRepository.OnHandRow::itemId).toList());
        String needle = ReportLookups.needle(q);

        List<StockSummaryResponse> result = rows.stream()
                .map(r -> toResponse(r, itemMap.get(r.itemId())))
                .filter(r -> needle == null || contains(r.itemSku(), needle) || contains(r.itemName(), needle))
                .sorted(Comparator.comparing(StockSummaryResponse::itemSku,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        return Paging.slice(result, page, size);
    }

    private static StockSummaryResponse toResponse(StockReportRepository.OnHandRow r, ItemResponse item) {
        BigDecimal other = r.onHand().subtract(r.storageQty()).subtract(r.receivingQty()).subtract(r.damagedQty());
        return new StockSummaryResponse(r.itemId(),
                item == null ? null : item.sku(), item == null ? null : item.name(),
                item == null ? null : item.baseUom(),
                r.onHand(), r.storageQty(), r.receivingQty(), r.damagedQty(), other);
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}

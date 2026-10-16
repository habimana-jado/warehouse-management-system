package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.reporting.dtos.StockSummaryResponse;

import java.util.UUID;

public interface StockSummaryService {
    PageResponse<StockSummaryResponse> summary(UUID warehouseId, String q, int page, int size);
}


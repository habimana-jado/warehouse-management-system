package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.reporting.dtos.ValuationReportResponse;

import java.util.UUID;

public interface ValuationReportService {
    ValuationReportResponse valuation(UUID warehouseId, String q, int page, int size);
}

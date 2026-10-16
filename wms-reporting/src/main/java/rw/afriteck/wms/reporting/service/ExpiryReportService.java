package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.reporting.dtos.ExpiryRowResponse;

import java.util.UUID;

public interface ExpiryReportService {
    PageResponse<ExpiryRowResponse> expiry(UUID warehouseId, Integer days, boolean includeExpired, int page, int size);
}

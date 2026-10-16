package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.reporting.dtos.OpenDemandRowResponse;
import rw.afriteck.wms.reporting.dtos.OutboundFulfillmentResponse;
import rw.afriteck.wms.reporting.dtos.PickingReportResponse;
import rw.afriteck.wms.reporting.dtos.ShipmentsReportResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface OutboundReportService {
    OutboundFulfillmentResponse fulfillment(UUID warehouseId, LocalDate from, LocalDate to);
    PickingReportResponse picking(UUID warehouseId, LocalDate from, LocalDate to);
    ShipmentsReportResponse shipments(UUID warehouseId, LocalDate from, LocalDate to);
    PageResponse<OpenDemandRowResponse> openDemand(UUID warehouseId, boolean shortfallOnly, int page, int size);
}
package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.inventory.dtos.LowStockResponse;

import java.util.List;
import java.util.UUID;

public interface LowStockReportService {
    List<LowStockResponse> list(UUID warehouseId);
    byte[] csv(UUID warehouseId);
}

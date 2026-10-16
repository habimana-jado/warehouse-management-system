package rw.afriteck.wms.reporting.service;

import rw.afriteck.wms.inventory.dtos.MovementFilter;

public interface StockLedgerReportService {
    byte[] exportCsv(MovementFilter filter);
}

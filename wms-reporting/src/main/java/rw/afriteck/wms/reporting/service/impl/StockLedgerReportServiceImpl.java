package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.reporting.service.StockLedgerReportService;
import rw.afriteck.wms.reporting.support.CsvWriter;
import rw.afriteck.wms.reporting.support.ReportingProperties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockLedgerReportServiceImpl implements StockLedgerReportService {

    private static final int PAGE_SIZE = 100;     // inventory caps page size at 100

    private final InventoryQueryService inventory;
    private final ReportingProperties props;

    @Override
    public byte[] exportCsv(MovementFilter filter) {
        if (filter.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        PageResponse<MovementHistoryResponse> first = inventory.history(filter, PageRequest.of(0, PAGE_SIZE));
        if (first.totalElements() > props.exportMaxRows()) {
            throw new BusinessException("EXPORT_TOO_LARGE", "The export has " + first.totalElements()
                    + " rows (limit " + props.exportMaxRows() + "). Narrow the date range or add filters.");
        }
        ZoneId zone = props.zoneId();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (CsvWriter csv = new CsvWriter(bos)) {
            csv.row("occurred_at", "type", "location", "sku", "item", "lot", "expiry_date", "serial",
                    "quantity", "balance_after", "unit_cost", "reference_type", "reference_number", "note",
                    "transaction_id");
            writeRows(csv, first.content(), zone);
            for (int p = 1; p < first.totalPages(); p++) {
                writeRows(csv, inventory.history(filter, PageRequest.of(p, PAGE_SIZE)).content(), zone);
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return bos.toByteArray();
    }

    private static void writeRows(CsvWriter csv, List<MovementHistoryResponse> rows, ZoneId zone) {
        for (MovementHistoryResponse m : rows) {
            csv.row(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(m.occurredAt().atZone(zone)), m.type(),
                    m.locationCode(), m.itemSku(), m.itemName(), m.lotNumber(), m.expiryDate(), m.serialNumber(),
                    m.quantity(), m.balanceAfter(), m.unitCost(), m.referenceType(), m.referenceNumber(),
                    m.note(), m.transactionId());
        }
    }
}

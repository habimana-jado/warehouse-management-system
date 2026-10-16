package rw.afriteck.wms.reporting.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.service.ReorderPolicyService;
import rw.afriteck.wms.reporting.service.LowStockReportService;
import rw.afriteck.wms.reporting.support.CsvWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LowStockReportServiceImpl implements LowStockReportService {

    private final ReorderPolicyService reorderPolicies;

    @Override
    public List<LowStockResponse> list(UUID warehouseId) {
        return reorderPolicies.findBelowMinimum(warehouseId);
    }

    @Override
    public byte[] csv(UUID warehouseId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (CsvWriter csv = new CsvWriter(bos)) {
            csv.row("sku", "name", "uom", "on_hand", "min_qty", "reorder_qty", "max_qty", "suggested_qty");
            for (LowStockResponse r : list(warehouseId)) {
                csv.row(r.itemSku(), r.itemName(), r.baseUom(), r.onHand(), r.minQty(), r.reorderQty(),
                        r.maxQty(), r.suggestedQty());
            }
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return bos.toByteArray();
    }
}

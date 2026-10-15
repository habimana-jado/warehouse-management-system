package rw.afriteck.wms.inbound.dtos;

import java.math.BigDecimal;

/** Result of scanning an item or box barcode against a PO (the receiver's handheld). */
public record PurchaseOrderLineScanResponse(
        PurchaseOrderLineResponse line,
        String uom,                                // the scanned unit (base UOM or a box)
        BigDecimal factorToBase) {}
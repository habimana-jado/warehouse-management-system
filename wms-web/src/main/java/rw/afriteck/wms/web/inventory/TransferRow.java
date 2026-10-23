package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.TransferSummaryResponse;

public record TransferRow(TransferSummaryResponse transfer, String sourceName, String destinationName) {}


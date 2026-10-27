package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.TransferSummaryResponse;

public record IncomingRow(TransferSummaryResponse transfer, String sourceName, String stage) {}

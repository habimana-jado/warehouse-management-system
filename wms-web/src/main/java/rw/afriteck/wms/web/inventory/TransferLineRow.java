package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.TransferLineResponse;

public record TransferLineRow(TransferLineResponse line,
                              boolean pending) {}       // still to be picked or moved (confirm form shown)

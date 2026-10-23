package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.TransferResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;

import java.util.List;

public record TransferView(TransferResponse transfer, String sourceName, String destinationName,
                           List<TransferLineRow> lines,
                           List<TransferLineRow> receiveLines,// lines still in transit, same order as the receive form
                           List<TaskResponse> tasks,
                           boolean canConfirm, boolean canShip, boolean canReceive, boolean canCancel,
                           boolean partiallyReceived) {}

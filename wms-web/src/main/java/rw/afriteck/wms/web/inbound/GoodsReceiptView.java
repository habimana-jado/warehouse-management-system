package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.inbound.dtos.GoodsReceiptResponse;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.web.inventory.BinOptions;

import java.util.List;

/**
 * Everything the receipt detail shows. canWork = the user may confirm put-away lines right now (manager or assignee, and the
 * task is ASSIGNED or IN_PROGRESS). storageBins is only filled when canWork.
 */
public record GoodsReceiptView(GoodsReceiptResponse receipt, TaskResponse task, boolean canWork,
                               List<BinOptions.BinOption> storageBins) {

    /** The put-away task exists but nobody has taken it yet. */
    public boolean taskNotStarted() {
        return task != null && "OPEN".equals(task.status().name());
    }
}
package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.PickListResponse;
import rw.afriteck.wms.web.inventory.BinOptions;

import java.util.List;
import java.util.UUID;

record PickListView(PickListResponse list, UUID taskId, String taskNumber, boolean open, boolean taskNotStarted,
                    boolean canCancel, boolean canOverrideExpiry,
                    List<PickLineView> lines, List<BinOptions.BinOption> storageBins) {
}

package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.CountResponse;
import rw.afriteck.wms.operations.dtos.TaskSummaryResponse;

import java.util.List;

public record CountView(CountResponse count, String warehouseName, String zoneName,
                        List<TaskSummaryResponse> tasks,
                        boolean canStart, boolean canSubmit, boolean canReopen, boolean canApprove,
                        boolean canCancel, boolean canCount, boolean canAddFound) {}

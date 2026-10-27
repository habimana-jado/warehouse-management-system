package rw.afriteck.wms.web.inbound;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptResponse;
import rw.afriteck.wms.inbound.enums.PutawayStatus;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.web.inventory.BinOptions;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class GoodsReceiptViews {

    private final GoodsReceiptService goodsReceipts;
    private final TaskService tasks;
    private final BinOptions binOptions;
    private final UserContext userContext;

    GoodsReceiptView load(UUID id) {
        GoodsReceiptResponse receipt = goodsReceipts.getById(id);          // NotFoundException -> error/404
        userContext.requireWarehouseAccess(receipt.warehouseId());
        TaskResponse task = receipt.taskId() == null ? null : tasks.getById(receipt.taskId());

        UUID me = userContext.currentUserId();
        boolean privileged = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        boolean readRole = privileged || userContext.hasRole("VIEWER") || userContext.hasRole("RECEIVER");
        boolean assignee = task != null && me.equals(task.assigneeId());
        if (!readRole && !assignee) {                                       // a picker sees only the receipt of their own task
            throw new ForbiddenException("This goods receipt is not assigned to you");
        }

        boolean taskActive = task != null
                && ("ASSIGNED".equals(task.status().name()) || "IN_PROGRESS".equals(task.status().name()));
        boolean hasPending = receipt.lines().stream().anyMatch(l -> l.putawayStatus() == PutawayStatus.PENDING);
        boolean canWork = hasPending && taskActive && (privileged || assignee);

        List<BinOptions.BinOption> storageBins = canWork
                ? binOptions.forWarehouse(receipt.warehouseId()).stream()
                .filter(b -> "STORAGE".equals(b.usage().name())).toList()
                : List.of();
        return new GoodsReceiptView(receipt, task, canWork, storageBins);
    }

    void fill(Model model, UUID id) {
        model.addAttribute("view", load(id));
    }
}
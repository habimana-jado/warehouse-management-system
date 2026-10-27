package rw.afriteck.wms.web.inbound;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;
import rw.afriteck.wms.inbound.service.GoodsReceiptService;
import rw.afriteck.wms.inbound.service.PurchaseOrderService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Builds the PurchaseOrderView and puts what the detail panel needs into the model. */
@Component
@RequiredArgsConstructor
class PurchaseOrderViews {

    private final PurchaseOrderService purchaseOrders;
    private final GoodsReceiptService goodsReceipts;
    private final UserContext userContext;
    private final Clock clock;

    PurchaseOrderView load(UUID id) {
        PurchaseOrderResponse po = purchaseOrders.getById(id);          // NotFoundException -> error/404
        userContext.requireWarehouseAccess(po.warehouseId());
        List<GoodsReceiptSummaryResponse> receipts = goodsReceipts.findByPurchaseOrder(id);

        boolean canManage = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        boolean canReceive = canManage || userContext.hasRole("RECEIVER");
        boolean waitingForGoods = po.status() == PurchaseOrderStatus.CONFIRMED
                || po.status() == PurchaseOrderStatus.PARTIALLY_RECEIVED;
        boolean overdue = waitingForGoods && po.expectedDate() != null
                && po.expectedDate().isBefore(LocalDate.now(clock));
        return new PurchaseOrderView(po, receipts, canManage, canReceive, overdue);
    }

    /** Adds "view" and, unless a failed cancel already put its bound form there, a fresh "cancelForm". */
    void fill(Model model, UUID id) {
        model.addAttribute("view", load(id));
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelPurchaseOrderForm());
        }
    }
}
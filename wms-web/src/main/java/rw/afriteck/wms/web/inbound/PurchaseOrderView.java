package rw.afriteck.wms.web.inbound;

import rw.afriteck.wms.inbound.dtos.GoodsReceiptSummaryResponse;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderResponse;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.util.List;

/** Everything the purchase order detail screen shows. canManage / canReceive come from the caller's roles. */
public record PurchaseOrderView(PurchaseOrderResponse po,
                                List<GoodsReceiptSummaryResponse> receipts,
                                boolean canManage,
                                boolean canReceive,
                                boolean overdue) {

    private PurchaseOrderStatus status() {
        return po.status();
    }

    public boolean canEdit() {
        return canManage && status() == PurchaseOrderStatus.DRAFT;
    }

    public boolean canConfirm() {
        return canManage && status() == PurchaseOrderStatus.DRAFT;
    }

    /** DRAFT or CONFIRMED (nothing received yet); the service refuses the rest. */
    public boolean canCancel() {
        return canManage && (status() == PurchaseOrderStatus.DRAFT || status() == PurchaseOrderStatus.CONFIRMED);
    }

    /** Close short: only a partially received order. */
    public boolean canClose() {
        return canManage && status() == PurchaseOrderStatus.PARTIALLY_RECEIVED;
    }

    public boolean canAdjustTolerance() {
        return canManage && PurchaseOrderStatus.RECEIVABLE.contains(status());
    }

    /** Slice 3 adds the Receive button that uses this. */
    public boolean canStartReceiving() {
        return canReceive && PurchaseOrderStatus.RECEIVABLE.contains(status());
    }
}
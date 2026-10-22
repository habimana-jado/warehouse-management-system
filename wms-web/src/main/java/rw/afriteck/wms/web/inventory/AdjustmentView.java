package rw.afriteck.wms.web.inventory;

import rw.afriteck.wms.inventory.dtos.AdjustmentResponse;

public record AdjustmentView(AdjustmentResponse adjustment,
                             boolean canApprove,      // manager, pending, not the requester
                             boolean canReject,       // manager, pending
                             boolean canCancel,       // requester, pending
                             boolean ownPendingByManager) {}   // manager looking at their own pending request (needs another approver)

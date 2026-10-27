package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.SalesOrderResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentSummaryResponse;

import java.util.List;

/**
 * Everything the order panel needs. The flags are computed now; slice 2 adds the buttons that use them.
 */
record SalesOrderView(SalesOrderResponse order, boolean overdue,
                      List<OrderPickRow> pickLists, List<ShipmentSummaryResponse> shipments,
                      boolean canEdit, boolean canConfirm, boolean canRelease, boolean canShip,
                      boolean canCancel, boolean canClose) {
}

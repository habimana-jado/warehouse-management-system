package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.ShipmentResponse;

record ShipmentView(ShipmentResponse shipment, boolean canConfirmDelivery) {
}

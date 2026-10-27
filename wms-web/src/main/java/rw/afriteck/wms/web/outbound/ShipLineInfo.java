package rw.afriteck.wms.web.outbound;

import rw.afriteck.wms.outbound.dtos.SalesOrderLineResponse;

import java.math.BigDecimal;

/**
 * What the ship form shows about each order line.
 */
record ShipLineInfo(String itemSku, String itemName, String baseUom,
                    BigDecimal orderedQty, BigDecimal shippedQty, BigDecimal readyQty) {
    static ShipLineInfo of(SalesOrderLineResponse l) {
        return new ShipLineInfo(l.itemSku(), l.itemName(), l.baseUom(), l.orderedQty(), l.shippedQty(), l.readyToShipQty());
    }
}

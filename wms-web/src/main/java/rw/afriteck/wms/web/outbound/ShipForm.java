package rw.afriteck.wms.web.outbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.CreateShipmentRequest;
import rw.afriteck.wms.outbound.dtos.SalesOrderLineResponse;
import rw.afriteck.wms.outbound.dtos.SalesOrderResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentLineRequest;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
class ShipForm {
    @Size(max = 500)
    private String shipToAddress;                                  // prefilled from the customer, editable
    @Size(max = 100)
    private String carrier;
    @Size(max = 100)
    private String trackingReference;
    @Min(1)
    private Integer packageCount;
    @Size(max = 1000)
    private String notes;
    @Valid
    private List<ShipLineForm> lines = new ArrayList<>();

    /**
     * One row per order line that has picked goods waiting, prefilled with that quantity.
     */
    static ShipForm forOrder(SalesOrderResponse o, String address) {
        ShipForm f = new ShipForm();
        f.setShipToAddress(address);
        for (SalesOrderLineResponse l : o.lines()) {
            if (l.readyToShipQty().signum() > 0) {
                ShipLineForm row = new ShipLineForm();
                row.setSalesOrderLineId(l.id());
                row.setQuantity(l.readyToShipQty());
                f.getLines().add(row);
            }
        }
        return f;
    }

    List<ShipmentLineRequest> requestLines() {
        return lines.stream()
                .filter(l -> l.getSalesOrderLineId() != null && l.getQuantity() != null && l.getQuantity().signum() > 0)
                .map(l -> new ShipmentLineRequest(l.getSalesOrderLineId(), l.getQuantity()))
                .toList();
    }

    CreateShipmentRequest toRequest() {
        return new CreateShipmentRequest(Codes.blankToNull(shipToAddress), Codes.blankToNull(carrier),
                Codes.blankToNull(trackingReference), packageCount, Codes.blankToNull(notes), requestLines());
    }
}

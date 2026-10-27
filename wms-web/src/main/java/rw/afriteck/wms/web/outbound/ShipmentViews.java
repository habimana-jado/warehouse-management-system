package rw.afriteck.wms.web.outbound;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.dtos.ShipmentResponse;
import rw.afriteck.wms.outbound.enums.ShipmentStatus;
import rw.afriteck.wms.outbound.service.ShipmentService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class ShipmentViews {

    private final ShipmentService shipments;
    private final UserContext userContext;

    void fill(Model model, UUID id) {
        userContext.requireWarehouseAccess(shipments.warehouseIdOf(id));
        ShipmentResponse s = shipments.getById(id);
        boolean manager = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        model.addAttribute("view", new ShipmentView(s, manager && s.status() == ShipmentStatus.DISPATCHED));
        if (!model.containsAttribute("deliveryForm")) {
            model.addAttribute("deliveryForm", new DeliveryForm());
        }
    }
}

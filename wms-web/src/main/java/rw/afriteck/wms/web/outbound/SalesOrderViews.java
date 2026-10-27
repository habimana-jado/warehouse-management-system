package rw.afriteck.wms.web.outbound;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.outbound.dtos.SalesOrderResponse;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.outbound.service.ShipmentService;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SalesOrderViews {

    private final SalesOrderService salesOrders;
    private final PickListService pickLists;
    private final ShipmentService shipments;
    private final UserContext userContext;
    private final Clock clock;

    /**
     * Checks warehouse access for the order's own warehouse, then puts the view in the model.
     */
    void fill(Model model, UUID id) {
        userContext.requireWarehouseAccess(salesOrders.warehouseIdOf(id));
        model.addAttribute("view", build(salesOrders.getById(id)));
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelSalesOrderForm());
        }
        if (!model.containsAttribute("releaseForm")) {
            model.addAttribute("releaseForm", new ReleaseForm());
        }
        model.addAttribute("priorities", List.of(TaskPriority.values()));
    }

    SalesOrderView build(SalesOrderResponse o) {
        boolean manager = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        SalesOrderStatus st = o.status();
        boolean releasableState = SalesOrderStatus.RELEASABLE.contains(st);
        boolean anyUnreleased = o.lines().stream().anyMatch(l -> l.unreleasedQty().signum() > 0);
        boolean anyReady = o.lines().stream().anyMatch(l -> l.readyToShipQty().signum() > 0);
        boolean anyPicked = o.lines().stream().anyMatch(l -> l.pickedQty().signum() > 0);
        boolean overdue = o.requiredDate() != null && o.requiredDate().isBefore(LocalDate.now(clock))
                && SalesOrderStatus.OPEN.contains(st);

        return new SalesOrderView(o, overdue,
                pickLists.findBySalesOrder(o.id()).stream().map(OrderPickRow::of).toList(),
                shipments.findBySalesOrder(o.id()),
                manager && st == SalesOrderStatus.DRAFT,                                     // edit
                manager && st == SalesOrderStatus.DRAFT,                                     // confirm
                manager && releasableState && anyUnreleased,                                 // release
                manager && releasableState && anyReady,                                      // ship
                manager && (st == SalesOrderStatus.DRAFT || st == SalesOrderStatus.CONFIRMED) && !anyPicked,   // cancel
                manager && st == SalesOrderStatus.PARTIALLY_SHIPPED && !anyReady);           // close short
    }
}

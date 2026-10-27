package rw.afriteck.wms.web.outbound;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.outbound.dtos.SalesOrderResponse;
import rw.afriteck.wms.outbound.dtos.ShipmentResponse;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
import rw.afriteck.wms.outbound.service.SalesOrderService;
import rw.afriteck.wms.outbound.service.ShipmentService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Flash;
import rw.afriteck.wms.web.support.FormErrors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/sales-orders/{id}/ship")
@PreAuthorize(WebAccess.MANAGER)
@RequiredArgsConstructor
class ShipmentFormController {

    private static final String VIEW = "outbound/ship-form";
    private static final Map<String, String> ERROR_FIELDS = Map.of("PACKAGE_COUNT_INVALID", "packageCount");

    private final SalesOrderService salesOrders;
    private final ShipmentService shipments;
    private final CustomerService customers;
    private final UserContext userContext;

    @GetMapping
    String form(@PathVariable UUID id, Model model) {
        SalesOrderResponse o = load(id);
        if (!shippable(o)) {
            return "redirect:/sales-orders/" + id;         // the detail hides the Ship button anyway
        }
        model.addAttribute("form", ShipForm.forOrder(o, customerAddress(o.customerId())));
        prepare(model, o);
        return VIEW;
    }

    @PostMapping
    String ship(@PathVariable UUID id, @Valid @ModelAttribute("form") ShipForm form, BindingResult result,
                Model model, RedirectAttributes redirect) {
        SalesOrderResponse o = load(id);
        if (!shippable(o)) {
            return "redirect:/sales-orders/" + id;
        }
        if (form.requestLines().isEmpty()) {
            result.reject("shipment.ship.nothing", "Enter the quantities to ship");
        }
        if (!result.hasErrors()) {
            try {
                ShipmentResponse s = shipments.create(id, form.toRequest());
                redirect.addFlashAttribute("flash", Flash.success("shipment.created", s.shipmentNumber()));
                return "redirect:/shipments/" + s.id();
            } catch (BusinessException e) {                // includes ConflictException (for example the order changed meanwhile)
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        prepare(model, o);
        return VIEW;
    }

    // ------------------------------------------------------------------ helpers

    private SalesOrderResponse load(UUID id) {
        SalesOrderResponse o = salesOrders.getById(id);                    // NotFoundException -> error/404
        userContext.requireWarehouseAccess(o.warehouseId());
        return o;
    }

    private static boolean shippable(SalesOrderResponse o) {
        return SalesOrderStatus.RELEASABLE.contains(o.status())
                && o.lines().stream().anyMatch(l -> l.readyToShipQty().signum() > 0);
    }

    private void prepare(Model model, SalesOrderResponse o) {
        Map<UUID, ShipLineInfo> info = new HashMap<>();
        o.lines().forEach(l -> info.put(l.id(), ShipLineInfo.of(l)));
        model.addAttribute("orderId", o.id());
        model.addAttribute("orderNumber", o.orderNumber());
        model.addAttribute("customerName", o.customerName());
        model.addAttribute("lineInfo", info);
    }

    /** The customer's address as one text, the way the service would copy it onto the delivery note. */
    private String customerAddress(UUID customerId) {
        CustomerResponse c = customers.getById(customerId);
        String joined = Stream.of(c.addressLine(), c.city(), c.country())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? null : joined;
    }
}

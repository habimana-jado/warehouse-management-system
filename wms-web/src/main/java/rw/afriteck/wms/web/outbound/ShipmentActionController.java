package rw.afriteck.wms.web.outbound;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.outbound.service.ShipmentService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;

import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

/**
 * Confirming delivery re-renders the whole panel.
 */
@Controller
@RequestMapping("/shipments")
@PreAuthorize(WebAccess.MANAGER)
@RequiredArgsConstructor
class ShipmentActionController {

    private static final Map<String, String> ERROR_FIELDS = Map.of("DELIVERY_DATE_INVALID", "deliveredAt");

    private final ShipmentService shipments;
    private final ShipmentViews views;
    private final UserContext userContext;

    @Value("${wms.ui.zone:Africa/Kigali}")
    private String zone;

    @PostMapping("/{id}/delivery")
    String confirmDelivery(@PathVariable UUID id,
                           @Valid @ModelAttribute("deliveryForm") DeliveryForm form, BindingResult result,
                           Model model, HttpServletRequest request) {
        userContext.requireWarehouseAccess(shipments.warehouseIdOf(id));
        if (!result.hasErrors()) {
            try {
                shipments.confirmDelivery(id, form.toRequest(ZoneId.of(zone)));
                model.addAttribute("notice", "shipment.delivered");
                model.addAttribute("deliveryForm", new DeliveryForm());
            } catch (BusinessException e) {                // includes ConflictException (already delivered)
                if (ERROR_FIELDS.containsKey(e.getCode())) {
                    result.rejectValue(ERROR_FIELDS.get(e.getCode()), e.getCode(), e.getMessage());
                } else {
                    model.addAttribute("error", e.getMessage());
                }
            }
        }
        views.fill(model, id);
        return Htmx.isFragmentRequest(request) ? "outbound/shipment-detail :: panel" : "outbound/shipment-detail";
    }
}

package rw.afriteck.wms.web.inventory;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

@Controller
@RequiredArgsConstructor
class StockMovementPageController {

    private final InventoryQueryService queries;
    private final WarehouseContext warehouseContext;
    private final BinOptions binOptions;
    private final ItemResolver itemResolver;
    private final UiLinks uiLinks;

    @Value("${wms.ui.zone:Africa/Kigali}")
    private String zone;                                          // same zone as the operations UI and reporting

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    @ModelAttribute("movementTypes")
    List<MovementType> movementTypes() {
        return List.of(MovementType.values());
    }

    @GetMapping("/stock/movements")
    @PreAuthorize(WebAccess.BOARD)
    String list(@ModelAttribute("filter") MovementFilterForm filter, BindingResult binding,
                @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        ZoneId zoneId = ZoneId.of(zone);

        String error = null;
        String errorArg = null;
        UUID itemId = null;
        if (StringUtils.hasText(filter.getItem())) {
            ItemResolver.Resolution match = itemResolver.resolve(filter.getItem());
            if (match.found()) {
                itemId = match.item().id();
            } else {
                error = match.ambiguous() ? "inventory.item.ambiguous" : "inventory.item.unknown";
                errorArg = match.ambiguous() ? match.candidateSkus() : filter.getItem();
            }
        }
        if (error == null && filter.getFrom() != null && filter.getTo() != null
                && filter.getFrom().isAfter(filter.getTo())) {
            error = "inventory.filter.dateRange";
        }

        PageView<MovementHistoryResponse> page;
        if (error != null) {
            page = InventoryPages.empty(pageable);
        } else {
            Instant from = filter.getFrom() == null ? null : filter.getFrom().atStartOfDay(zoneId).toInstant();
            Instant to = filter.getTo() == null ? null : filter.getTo().plusDays(1).atStartOfDay(zoneId).toInstant();   // exclusive
            Set<MovementType> types = filter.getType() == null ? null : EnumSet.of(filter.getType());
            MovementFilter movementFilter = new MovementFilter(warehouseId, itemId, filter.getLocationId(), null,
                    filter.getSerial(), types, from, to, null, null);
            page = PageView.of(queries.history(movementFilter, pageable), Function.identity());
        }
        model.addAttribute("page", page);
        model.addAttribute("error", error);
        model.addAttribute("errorArg", errorArg);
        InventoryPages.sortLinks(model, uiLinks, "occurredAt", "quantity");

        if (Htmx.isFragmentRequest(request)) {
            return "inventory/movements :: results";
        }
        model.addAttribute("bins", binOptions.forWarehouse(warehouseId));
        return "inventory/movements";
    }
}

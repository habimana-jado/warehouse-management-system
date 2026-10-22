package rw.afriteck.wms.web.inventory;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Controller
@RequiredArgsConstructor
class StockPageController {

    private final InventoryQueryService queries;
    private final WarehouseContext warehouseContext;
    private final BinOptions binOptions;
    private final ItemResolver itemResolver;
    private final UiLinks uiLinks;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    @GetMapping("/stock")
    @PreAuthorize(WebAccess.ANY_USER)
    String list(@ModelAttribute("filter") StockFilterForm filter, BindingResult binding,
                @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();

        UUID itemId = null;
        String itemProblem = null;
        String itemProblemArg = null;
        if (StringUtils.hasText(filter.getItem())) {
            ItemResolver.Resolution match = itemResolver.resolve(filter.getItem());
            if (match.found()) {
                itemId = match.item().id();
            } else {
                itemProblem = match.ambiguous() ? "inventory.item.ambiguous" : "inventory.item.unknown";
                itemProblemArg = match.ambiguous() ? match.candidateSkus() : filter.getItem();
            }
        }

        PageView<StockLevelResponse> page;
        if (itemProblem != null) {
            page = InventoryPages.empty(pageable);
        } else {
            StockFilter stockFilter = new StockFilter(warehouseId, filter.getLocationId(), itemId, null,
                    filter.getSerial(), filter.isIncludeZero());
            page = PageView.of(queries.search(stockFilter, pageable), Function.identity());
        }
        model.addAttribute("page", page);
        model.addAttribute("itemProblem", itemProblem);
        model.addAttribute("itemProblemArg", itemProblemArg);
        InventoryPages.sortLinks(model, uiLinks, "quantity", "updatedAt");

        if (Htmx.isFragmentRequest(request)) {
            return "inventory/stock :: results";
        }
        model.addAttribute("bins", binOptions.forWarehouse(warehouseId));
        return "inventory/stock";
    }
}

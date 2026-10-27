package rw.afriteck.wms.web.inbound;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.inbound.dtos.CustomerReturnLineResponse;
import rw.afriteck.wms.inbound.dtos.CustomerReturnResponse;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.service.CustomerReturnService;
import rw.afriteck.wms.web.inventory.BinOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class CustomerReturnViews {

    private final CustomerReturnService returns;
    private final ItemService itemService;
    private final BinOptions binOptions;
    private final UserContext userContext;

    CustomerReturnView load(UUID id) {
        CustomerReturnResponse ret = returns.getById(id);                  // NotFoundException -> error/404
        userContext.requireWarehouseAccess(ret.warehouseId());

        boolean canManage = userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
        boolean canReceive = canManage || userContext.hasRole("RECEIVER");

        Set<UUID> itemIds = ret.lines().stream().map(CustomerReturnLineResponse::itemId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> items = itemIds.isEmpty()
                ? Map.of()
                : itemService.findAllByIds(itemIds).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
        Map<UUID, ReturnLineInfo> info = new HashMap<>();
        for (CustomerReturnLineResponse l : ret.lines()) {
            ItemResponse item = items.get(l.itemId());
            info.put(l.id(), new ReturnLineInfo(item == null ? "NONE" : item.trackingType().name(),
                    item != null && item.expiryTracked()));
        }

        boolean receiving = canReceive && ret.status() == CustomerReturnStatus.EXPECTED;
        List<BinOptions.BinOption> bins = receiving ? binOptions.forWarehouse(ret.warehouseId()) : List.of();
        return new CustomerReturnView(ret, canManage, canReceive, info,
                bins.stream().filter(b -> "STORAGE".equals(b.usage().name())).toList(),
                bins.stream().filter(b -> "DAMAGED".equals(b.usage().name())).toList());
    }

    /** Adds "view" plus the two forms, unless a failed submit already put its bound form there. */
    void fill(Model model, UUID id) {
        CustomerReturnView view = load(id);
        model.addAttribute("view", view);
        if (!model.containsAttribute("receiveForm")) {
            model.addAttribute("receiveForm", ReceiveReturnForm.from(view.ret()));
        }
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelReturnForm());
        }
    }
}
package rw.afriteck.wms.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.service.InventoryQueryService;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inventory/stocks")
@RequiredArgsConstructor
public class StockController {

    private final InventoryQueryService queries;
    private final UserContext userContext;

    @GetMapping
    @PreAuthorize(InventoryAccess.ANY_USER)
    public PageResponse<StockLevelResponse> search(
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) UUID lotId,
            @RequestParam(required = false) String serial,
            @RequestParam(defaultValue = "false") boolean includeZero,
            Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return queries.search(new StockFilter(warehouseId, locationId, itemId, lotId, serial, includeZero), pageable);
    }

    /** Bin contents, e.g. after the scanner resolves a bin barcode through /api/warehouses/{id}/locations/by-barcode/{barcode}. */
    @GetMapping("/locations/{locationId}")
    @PreAuthorize(InventoryAccess.ANY_USER)
    public List<StockLevelResponse> inLocation(@PathVariable UUID locationId, @RequestParam UUID warehouseId) {
        userContext.requireWarehouseAccess(warehouseId);
        return queries.findInLocation(warehouseId, locationId);
    }

    /** Where is this item, lot by lot? */
    @GetMapping("/items/{itemId}/availability")
    @PreAuthorize(InventoryAccess.ANY_USER)
    public List<StockLevelResponse> availability(@PathVariable UUID itemId, @RequestParam UUID warehouseId) {
        userContext.requireWarehouseAccess(warehouseId);
        return queries.findAvailable(warehouseId, itemId);
    }

    @GetMapping("/movements")
    @PreAuthorize(InventoryAccess.BOARD)
    public PageResponse<MovementHistoryResponse> movements(
            @RequestParam UUID warehouseId,
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) UUID lotId,
            @RequestParam(required = false) String serial,
            @RequestParam(required = false) Set<MovementType> types,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) UUID referenceId,
            Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return queries.history(new MovementFilter(warehouseId, itemId, locationId, lotId, serial, types,
                from, to, referenceType, referenceId), pageable);
    }
}

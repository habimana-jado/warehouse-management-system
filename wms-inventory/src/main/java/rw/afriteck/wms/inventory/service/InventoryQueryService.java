package rw.afriteck.wms.inventory.service;

import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface InventoryQueryService {

    // ---- the methods other modules (inbound, outbound, reporting) may call ----

    /**
     * Lot-by-lot, bin-by-bin stock of one item in a warehouse (quantity > 0). Includes locationUsage so callers can pick STORAGE bins.
     */
    List<StockLevelResponse> findAvailable(UUID warehouseId, UUID itemId);

    /**
     * Total quantity in ALL bins of the warehouse. Zero when the item has no stock.
     */
    BigDecimal getOnHand(UUID warehouseId, UUID itemId);

    /**
     * Batch version. Every requested item id is present in the map (ZERO when no stock).
     */
    Map<UUID, BigDecimal> getOnHand(UUID warehouseId, Collection<UUID> itemIds);

    /**
     * Quantity in STORAGE bins only: what an order-confirmation availability warning should use. Same map rules.
     */
    Map<UUID, BigDecimal> getAvailableInStorage(UUID warehouseId, Collection<UUID> itemIds);

    /**
     * Inventory's own controllers
     */
    PageResponse<StockLevelResponse> search(StockFilter filter, Pageable pageable);

    List<StockLevelResponse> findInLocation(UUID warehouseId, UUID locationId);       // bin contents (scanner)

    PageResponse<MovementHistoryResponse> history(MovementFilter filter, Pageable pageable);
}

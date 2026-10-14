package rw.afriteck.wms.inventory.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.StockBalance;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockBalanceRepository extends JpaRepository<StockBalance, UUID>, JpaSpecificationExecutor<StockBalance> {

    /**
     * Projection for grouped sums.
     */
    interface ItemQuantity {
        UUID getItemId();

        BigDecimal getQuantity();
    }

    // ---------- locked lookups used by InventoryService.post (call inside a transaction) ----------
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBalance b
            where b.locationId = :locationId and b.itemId = :itemId
              and b.lot is null and b.serialNumber is null""")
    Optional<StockBalance> lockUntracked(@Param("locationId") UUID locationId, @Param("itemId") UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBalance b
            where b.locationId = :locationId and b.itemId = :itemId
              and b.lot.id = :lotId and b.serialNumber is null""")
    Optional<StockBalance> lockLot(@Param("locationId") UUID locationId, @Param("itemId") UUID itemId,
                                   @Param("lotId") UUID lotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from StockBalance b
            where b.locationId = :locationId and b.itemId = :itemId and b.serialNumber = :serial""")
    Optional<StockBalance> lockSerial(@Param("locationId") UUID locationId, @Param("itemId") UUID itemId,
                                      @Param("serial") String serial);

    /**
     * Where is this serial right now? (receipt duplicate check, serial issue). Call with BigDecimal.ZERO.
     */
    Optional<StockBalance> findByItemIdAndSerialNumberAndQuantityGreaterThan(UUID itemId, String serial, BigDecimal min);

    // ---------- reads ----------
    @Query("""
            select b from StockBalance b left join fetch b.lot
            where b.locationId = :locationId and b.quantity > 0
            order by b.itemId""")
    List<StockBalance> findInLocation(@Param("locationId") UUID locationId);          // bin contents

    /**
     * Lot-by-lot stock of one item: what outbound uses to show the picker where the item is.
     */
    @Query("""
            select b from StockBalance b left join fetch b.lot
            where b.warehouseId = :warehouseId and b.itemId = :itemId and b.quantity > 0
            order by b.locationId""")
    List<StockBalance> findAvailable(@Param("warehouseId") UUID warehouseId, @Param("itemId") UUID itemId);

    @Query(value = """
            select coalesce(sum(quantity), 0) from stock_balance
            where warehouse_id = :warehouseId and item_id = :itemId""", nativeQuery = true)
    BigDecimal sumQuantity(@Param("warehouseId") UUID warehouseId, @Param("itemId") UUID itemId);

    /**
     * itemIds must not be empty. Items without stock are simply absent from the result.
     */
    @Query(value = """
            select item_id as itemId, sum(quantity) as quantity from stock_balance
            where warehouse_id = :warehouseId and item_id in (:itemIds) and quantity > 0
            group by item_id""", nativeQuery = true)
    List<ItemQuantity> sumByItems(@Param("warehouseId") UUID warehouseId, @Param("itemIds") Collection<UUID> itemIds);

    /**
     * Same, restricted to given bins (e.g. STORAGE bins only, for availability). Neither list may be empty.
     */
    @Query(value = """
            select item_id as itemId, sum(quantity) as quantity from stock_balance
            where location_id in (:locationIds) and item_id in (:itemIds) and quantity > 0
            group by item_id""", nativeQuery = true)
    List<ItemQuantity> sumByItemsInLocations(@Param("locationIds") Collection<UUID> locationIds,
                                             @Param("itemIds") Collection<UUID> itemIds);

    @Query("""
            select b.quantity from StockBalance b
            where b.locationId = :locationId and b.itemId = :itemId and b.lot is null and b.serialNumber is null""")
    Optional<BigDecimal> quantityUntracked(@Param("locationId") UUID locationId, @Param("itemId") UUID itemId);

    @Query("""
            select b.quantity from StockBalance b
            where b.locationId = :locationId and b.itemId = :itemId and b.lot.id = :lotId and b.serialNumber is null""")
    Optional<BigDecimal> quantityLot(@Param("locationId") UUID locationId, @Param("itemId") UUID itemId,
                                     @Param("lotId") UUID lotId);

    @Query("""
            select b.quantity from StockBalance b
            where b.locationId = :locationId and b.itemId = :itemId and b.serialNumber = :serial""")
    Optional<BigDecimal> quantitySerial(@Param("locationId") UUID locationId, @Param("itemId") UUID itemId,
                                        @Param("serial") String serial);

    @Query("""
        select b from StockBalance b left join fetch b.lot
        where b.warehouseId = :warehouseId and b.quantity > 0""")
    List<StockBalance> findAllPositive(@Param("warehouseId") UUID warehouseId);

    @Query("""
        select b from StockBalance b left join fetch b.lot
        where b.locationId in :locationIds and b.quantity > 0""")
    List<StockBalance> findPositiveInLocations(@Param("locationIds") Collection<UUID> locationIds);   // never empty

    // ---------- DeactivationGuard (pass BigDecimal.ZERO) ----------
    boolean existsByLocationIdAndQuantityGreaterThan(UUID locationId, BigDecimal min);

    boolean existsByItemIdAndQuantityGreaterThan(UUID itemId, BigDecimal min);

    boolean existsByWarehouseIdAndQuantityGreaterThan(UUID warehouseId, BigDecimal min);
}

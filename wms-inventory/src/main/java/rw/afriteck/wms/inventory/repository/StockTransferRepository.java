package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.StockTransfer;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.enums.TransferStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {

    @Query(value = "select nextval('transfer_number_seq')", nativeQuery = true)
    long nextNumber();

    @EntityGraph(attributePaths = "lines")
    Optional<StockTransfer> findWithLinesById(UUID id);

    /** Shows a transfer to BOTH the sending and the receiving warehouse. */
    @Query(value = """
            select t from StockTransfer t
            where (t.sourceWarehouseId = :warehouseId or t.destinationWarehouseId = :warehouseId)
              and t.status in :statuses and lower(t.transferNumber) like :pattern""",
            countQuery = """
            select count(t) from StockTransfer t
            where (t.sourceWarehouseId = :warehouseId or t.destinationWarehouseId = :warehouseId)
              and t.status in :statuses and lower(t.transferNumber) like :pattern""")
    Page<StockTransfer> search(@Param("warehouseId") UUID warehouseId,
                               @Param("statuses") Collection<TransferStatus> statuses,
                               @Param("pattern") String pattern, Pageable pageable);

    interface LineCount { UUID getTransferId(); long getLineCount(); }

    @Query(value = """
            select transfer_id as transferId, count(*) as lineCount from stock_transfer_line
            where transfer_id in (:ids) group by transfer_id""", nativeQuery = true)
    List<LineCount> countLines(@Param("ids") Collection<UUID> ids);

    // ---- DeactivationGuard (statuses: PENDING, IN_TRANSIT). Written as @Query on purpose: a derived
    // "...SourceWarehouseIdOr...DestinationWarehouseIdAndStatusIn" would bind as (A) OR (B AND C).
    @Query("""
            select count(t) from StockTransfer t
            where (t.sourceWarehouseId = :warehouseId or t.destinationWarehouseId = :warehouseId)
              and t.status in :statuses""")
    long countOpenForWarehouse(@Param("warehouseId") UUID warehouseId,
                               @Param("statuses") Collection<TransferStatus> statuses);

    @Query("""
            select count(l) from StockTransferLine l
            where l.itemId = :itemId and l.transfer.status in :statuses""")
    long countOpenLinesForItem(@Param("itemId") UUID itemId,
                               @Param("statuses") Collection<TransferStatus> statuses);

    @Query("""
            select count(l) from StockTransferLine l
            where (l.fromLocationId = :locationId or l.toLocationId = :locationId) and l.transfer.status in :statuses""")
    long countOpenLinesForLocation(@Param("locationId") UUID locationId,
                                   @Param("statuses") Collection<TransferStatus> statuses);
}
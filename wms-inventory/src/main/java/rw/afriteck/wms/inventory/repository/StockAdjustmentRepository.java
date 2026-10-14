package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.StockAdjustment;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {

    @Query(value = "select nextval('adjustment_number_seq')", nativeQuery = true)
    long nextNumber();

    @EntityGraph(attributePaths = {"reason", "lines"})          // detail screen only, never combined with paging
    Optional<StockAdjustment> findWithLinesById(UUID id);

    @Query(value = """
            select a from StockAdjustment a join fetch a.reason
            where a.warehouseId = :warehouseId and a.status in :statuses
              and (lower(a.adjustmentNumber) like :pattern or lower(coalesce(a.notes, '')) like :pattern)""",
            countQuery = """
            select count(a) from StockAdjustment a
            where a.warehouseId = :warehouseId and a.status in :statuses
              and (lower(a.adjustmentNumber) like :pattern or lower(coalesce(a.notes, '')) like :pattern)""")
    Page<StockAdjustment> search(@Param("warehouseId") UUID warehouseId,
                                 @Param("statuses") Collection<AdjustmentStatus> statuses,
                                 @Param("pattern") String pattern, Pageable pageable);

    @Query(value = """
            select a from StockAdjustment a join fetch a.reason
            where a.warehouseId = :warehouseId and a.requestedBy = :requestedBy and a.status in :statuses
              and (lower(a.adjustmentNumber) like :pattern or lower(coalesce(a.notes, '')) like :pattern)""",
            countQuery = """
            select count(a) from StockAdjustment a
            where a.warehouseId = :warehouseId and a.requestedBy = :requestedBy and a.status in :statuses
              and (lower(a.adjustmentNumber) like :pattern or lower(coalesce(a.notes, '')) like :pattern)""")
    Page<StockAdjustment> searchByRequester(@Param("warehouseId") UUID warehouseId,
                                            @Param("requestedBy") UUID requestedBy,
                                            @Param("statuses") Collection<AdjustmentStatus> statuses,
                                            @Param("pattern") String pattern, Pageable pageable);

    interface LineCount { UUID getAdjustmentId(); long getLineCount(); }

    @Query(value = """
            select adjustment_id as adjustmentId, count(*) as lineCount from stock_adjustment_line
            where adjustment_id in (:ids) group by adjustment_id""", nativeQuery = true)
    List<LineCount> countLines(@Param("ids") Collection<UUID> ids);

    // ---- DeactivationGuard ----
    boolean existsByWarehouseIdAndStatus(UUID warehouseId, AdjustmentStatus status);

    @Query("select count(l) from StockAdjustmentLine l where l.locationId = :locationId and l.adjustment.status = :status")
    long countLinesForLocation(@Param("locationId") UUID locationId, @Param("status") AdjustmentStatus status);

    @Query("select count(l) from StockAdjustmentLine l where l.itemId = :itemId and l.adjustment.status = :status")
    long countLinesForItem(@Param("itemId") UUID itemId, @Param("status") AdjustmentStatus status);
}
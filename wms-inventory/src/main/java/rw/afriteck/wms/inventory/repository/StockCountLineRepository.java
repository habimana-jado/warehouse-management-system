package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.StockCountLine;
import rw.afriteck.wms.inventory.enums.CountLineStatus;
import rw.afriteck.wms.inventory.enums.CountStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockCountLineRepository extends JpaRepository<StockCountLine, UUID> {

    @EntityGraph(attributePaths = "lot")
    Page<StockCountLine> findByCountId(UUID countId, Pageable pageable);

    Optional<StockCountLine> findByIdAndCountId(UUID id, UUID countId);
    long countByCountIdAndStatus(UUID countId, CountLineStatus status);

    interface LineStats { UUID getCountId(); long getTotal(); long getCounted(); }

    @Query(value = """
            select count_id as countId, count(*) as total,
                   count(*) filter (where status = 'COUNTED') as counted
            from stock_count_line where count_id in (:ids) group by count_id""", nativeQuery = true)
    List<LineStats> stats(@Param("ids") Collection<UUID> ids);

    @Query("""
            select count(l) from StockCountLine l
            where l.count.id = :countId and l.countedQty is not null and l.countedQty <> l.expectedQty""")
    long countVariance(@Param("countId") UUID countId);

    @Query(value = """
            select l from StockCountLine l left join fetch l.lot
            where l.count.id = :countId and l.countedQty is not null and l.countedQty <> l.expectedQty""",
            countQuery = """
            select count(l) from StockCountLine l
            where l.count.id = :countId and l.countedQty is not null and l.countedQty <> l.expectedQty""")
    Page<StockCountLine> findVariance(@Param("countId") UUID countId, Pageable pageable);

    @Query("""
            select l from StockCountLine l left join fetch l.lot
            where l.count.id = :countId and l.countedQty is not null and l.countedQty <> l.expectedQty""")
    List<StockCountLine> findAllVariance(@Param("countId") UUID countId);

    @Query("select coalesce(max(l.lineNo), 0) from StockCountLine l where l.count.id = :countId")
    int maxLineNo(@Param("countId") UUID countId);

    // ---- DeactivationGuard (statuses: IN_PROGRESS, REVIEW) ----
    @Query("select count(l) from StockCountLine l where l.locationId = :locationId and l.count.status in :statuses")
    long countOpenForLocation(@Param("locationId") UUID locationId, @Param("statuses") Collection<CountStatus> statuses);

    @Query("select count(l) from StockCountLine l where l.itemId = :itemId and l.count.status in :statuses")
    long countOpenForItem(@Param("itemId") UUID itemId, @Param("statuses") Collection<CountStatus> statuses);
}
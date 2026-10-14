package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.StockCount;
import rw.afriteck.wms.inventory.enums.CountStatus;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StockCountRepository extends JpaRepository<StockCount, UUID> {

    @Query(value = "select nextval('count_number_seq')", nativeQuery = true)
    long nextNumber();

    @Query(value = """
            select c from StockCount c
            where c.warehouseId = :warehouseId and c.status in :statuses and lower(c.countNumber) like :pattern""",
            countQuery = """
            select count(c) from StockCount c
            where c.warehouseId = :warehouseId and c.status in :statuses and lower(c.countNumber) like :pattern""")
    Page<StockCount> search(@Param("warehouseId") UUID warehouseId,
                            @Param("statuses") Collection<CountStatus> statuses,
                            @Param("pattern") String pattern, Pageable pageable);

    List<StockCount> findByWarehouseIdAndStatusIn(UUID warehouseId, Collection<CountStatus> statuses);   // overlap check

    boolean existsByWarehouseIdAndStatusIn(UUID warehouseId, Collection<CountStatus> statuses);          // guard
}
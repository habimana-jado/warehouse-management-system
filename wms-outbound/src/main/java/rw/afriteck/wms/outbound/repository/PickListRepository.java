package rw.afriteck.wms.outbound.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.outbound.entity.PickList;
import rw.afriteck.wms.outbound.enums.PickLineStatus;
import rw.afriteck.wms.outbound.enums.PickListStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickListRepository extends JpaRepository<PickList, UUID> {

    Optional<PickList> findByTaskId(UUID taskId);                                   // worker opens the list from the task
    List<PickList> findBySalesOrderIdOrderByReleasedAtDesc(UUID salesOrderId);
    boolean existsBySalesOrderIdAndStatusIn(UUID salesOrderId, Collection<PickListStatus> statuses);

    // to-one fetch is safe with paging; pass a non-empty status set
    @Query(value = "select p from PickList p join fetch p.salesOrder "
            + "where p.warehouseId = :wh and p.status in :statuses",
            countQuery = "select count(p) from PickList p where p.warehouseId = :wh and p.status in :statuses")
    Page<PickList> search(@Param("wh") UUID warehouseId,
                          @Param("statuses") Collection<PickListStatus> statuses, Pageable pageable);

    @Query("select l.pickList.id as parentId, count(l) as total from PickLine l "
            + "where l.pickList.id in :ids group by l.pickList.id")
    List<ParentCount> countLines(@Param("ids") Collection<UUID> ids);

    // the status is a parameter, never a qualified enum literal in the query text
    @Query("select l.pickList.id as parentId, count(l) as total from PickLine l "
            + "where l.pickList.id in :ids and l.status = :status group by l.pickList.id")
    List<ParentCount> countLinesByStatus(@Param("ids") Collection<UUID> ids, @Param("status") PickLineStatus status);
}

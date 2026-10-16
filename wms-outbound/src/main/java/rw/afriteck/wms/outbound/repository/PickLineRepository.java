package rw.afriteck.wms.outbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.outbound.entity.PickAllocation;
import rw.afriteck.wms.outbound.entity.PickLine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickLineRepository extends JpaRepository<PickLine, UUID> {

    /** Pick confirmation: find the order id first, lock the order header, then work on the line (lock order rule). */
    @Query("select l.pickList.salesOrder.id from PickLine l where l.id = :id")
    Optional<UUID> findSalesOrderIdById(@Param("id") UUID id);

    Optional<PickLine> findByTaskLineId(UUID taskLineId);
}


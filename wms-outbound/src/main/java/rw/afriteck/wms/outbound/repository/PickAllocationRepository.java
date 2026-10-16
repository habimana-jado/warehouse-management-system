package rw.afriteck.wms.outbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.outbound.entity.PickAllocation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PickAllocationRepository extends JpaRepository<PickAllocation, UUID> {

    List<PickAllocation> findByPickLine_IdIn(Collection<UUID> pickLineIds);          // callers chunk big lists

    /** Lot and serial traceability of everything picked for an order (delivery note, recall queries). */
    @Query("select a from PickAllocation a where a.pickLine.pickList.salesOrder.id = :orderId")
    List<PickAllocation> findBySalesOrderId(@Param("orderId") UUID orderId);
}

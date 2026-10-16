package rw.afriteck.wms.outbound.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.SalesOrderFilter;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;
import rw.afriteck.wms.outbound.enums.SalesOrderStatus;

import java.util.*;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SalesOrder o where o.id = :id")
    Optional<SalesOrder> findByIdForUpdate(@Param("id") UUID id);

    // guards (pass non-empty status sets)
    boolean existsByCustomerIdAndStatusIn(UUID customerId, Collection<SalesOrderStatus> statuses);
    boolean existsByLines_ItemIdAndStatusIn(UUID itemId, Collection<SalesOrderStatus> statuses);

    // list screens: one grouped query per page, never a collection fetch with paging (callers chunk with Chunks.partition)
    @Query("""
            select l.salesOrder.id as orderId, count(l) as lineCount,
                   sum(l.orderedQty) as orderedQty, sum(l.pickedQty) as pickedQty, sum(l.shippedQty) as shippedQty
            from SalesOrderLine l
            where l.salesOrder.id in :ids
            group by l.salesOrder.id
            """)
    List<OrderTotals> totalsByOrder(@Param("ids") Collection<UUID> ids);
}


package rw.afriteck.wms.inbound.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, UUID> {

    /** Progress figures for the PO list screen. */
    interface Totals {
        UUID getPurchaseOrderId();
        Long getLineCount();
        BigDecimal getOrderedQty();
        BigDecimal getAcceptedQty();
    }

    /** One grouped query for a page of POs (page size is capped at 100, so no chunking needed). */
    @Query("""
            select l.purchaseOrder.id as purchaseOrderId,
                   count(l)           as lineCount,
                   sum(l.orderedQty)  as orderedQty,
                   sum(l.acceptedQty) as acceptedQty
              from PurchaseOrderLine l
             where l.purchaseOrder.id in :orderIds
             group by l.purchaseOrder.id
            """)
    List<Totals> totalsByOrder(@Param("orderIds") Collection<UUID> orderIds);
}

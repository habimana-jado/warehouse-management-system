package rw.afriteck.wms.outbound.repository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.SalesOrderFilter;
import rw.afriteck.wms.outbound.entity.SalesOrder;
import rw.afriteck.wms.outbound.entity.SalesOrderLine;

import java.util.ArrayList;
import java.util.List;

public final class SalesOrderSpecs {
    private SalesOrderSpecs() {}

    public static Specification<SalesOrder> from(SalesOrderFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("warehouseId"), f.warehouseId()));
            if (f.statuses() != null && !f.statuses().isEmpty()) p.add(root.get("status").in(f.statuses()));
            if (f.customerId() != null) p.add(cb.equal(root.get("customerId"), f.customerId()));
            if (f.itemId() != null) {
                Subquery<Integer> sq = query.subquery(Integer.class);
                Root<SalesOrderLine> l = sq.from(SalesOrderLine.class);
                sq.select(cb.literal(1)).where(cb.equal(l.get("salesOrder"), root), cb.equal(l.get("itemId"), f.itemId()));
                p.add(cb.exists(sq));
            }
            if (f.orderDateFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("orderDate"), f.orderDateFrom()));
            if (f.orderDateTo() != null) p.add(cb.lessThanOrEqualTo(root.get("orderDate"), f.orderDateTo()));
            if (f.q() != null && !f.q().isBlank()) {
                String pattern = Codes.likePattern(f.q());
                p.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("customerReference"), "")), pattern)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}

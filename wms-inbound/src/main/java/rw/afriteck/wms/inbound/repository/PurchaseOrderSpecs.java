package rw.afriteck.wms.inbound.repository;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.PurchaseOrderFilter;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.entity.PurchaseOrderLine;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PurchaseOrderSpecs {

    private PurchaseOrderSpecs() {}

    public static Specification<PurchaseOrder> of(PurchaseOrderFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();

            p.add(cb.equal(root.get("warehouseId"), f.warehouseId()));

            if (f.statuses() != null && !f.statuses().isEmpty()) {
                p.add(root.get("status").in(f.statuses()));
            }
            if (f.supplierId() != null) {
                p.add(cb.equal(root.get("supplierId"), f.supplierId()));
            }
            if (f.orderDateFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("orderDate"), f.orderDateFrom()));
            }
            if (f.orderDateTo() != null) {
                p.add(cb.lessThanOrEqualTo(root.<LocalDate>get("orderDate"), f.orderDateTo()));
            }
            if (f.itemId() != null) {
                // subquery instead of a join: no duplicate rows, no distinct, paging stays correct
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<PurchaseOrderLine> line = sub.from(PurchaseOrderLine.class);
                sub.select(cb.literal(1))
                        .where(cb.equal(line.get("purchaseOrder"), root),
                                cb.equal(line.get("itemId"), f.itemId()));
                p.add(cb.exists(sub));
            }
            if (f.q() != null && !f.q().isBlank()) {
                // never pass a null into lower(...): the pattern is only built for non-blank text
                String pattern = Codes.likePattern(f.q()).toLowerCase(Locale.ROOT);
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("poNumber")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("supplierReference"), "")), pattern)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
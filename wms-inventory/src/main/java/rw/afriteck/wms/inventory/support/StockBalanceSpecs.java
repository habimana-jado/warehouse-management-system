package rw.afriteck.wms.inventory.support;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.entity.StockBalance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class StockBalanceSpecs {
    private StockBalanceSpecs() {}

    public static Specification<StockBalance> of(StockFilter f) {
        return (root, query, cb) -> {
            // join the lot in the same query (ManyToOne, safe with paging); never in the count query
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("lot", JoinType.LEFT);
            }
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("warehouseId"), f.warehouseId()));
            if (f.locationId() != null)   ps.add(cb.equal(root.get("locationId"), f.locationId()));
            if (f.itemId() != null)       ps.add(cb.equal(root.get("itemId"), f.itemId()));
            if (f.lotId() != null)        ps.add(cb.equal(root.get("lot").get("id"), f.lotId()));
            if (f.serialNumber() != null) ps.add(cb.equal(root.get("serialNumber"), f.serialNumber()));
            if (!f.includeZero())         ps.add(cb.greaterThan(root.<BigDecimal>get("quantity"), BigDecimal.ZERO));
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}
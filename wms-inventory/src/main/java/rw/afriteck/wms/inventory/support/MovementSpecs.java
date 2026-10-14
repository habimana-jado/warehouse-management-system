package rw.afriteck.wms.inventory.support;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.entity.StockMovement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class MovementSpecs {
    private MovementSpecs() {
    }

    public static Specification<StockMovement> of(MovementFilter f) {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("lot", JoinType.LEFT);
            }
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("warehouseId"), f.warehouseId()));
            if (f.itemId() != null) ps.add(cb.equal(root.get("itemId"), f.itemId()));
            if (f.locationId() != null) ps.add(cb.equal(root.get("locationId"), f.locationId()));
            if (f.lotId() != null) ps.add(cb.equal(root.get("lot").get("id"), f.lotId()));
            if (f.serialNumber() != null) ps.add(cb.equal(root.get("serialNumber"), f.serialNumber()));
            if (f.types() != null && !f.types().isEmpty()) ps.add(root.get("type").in(f.types()));
            if (f.from() != null) ps.add(cb.greaterThanOrEqualTo(root.<Instant>get("occurredAt"), f.from()));
            if (f.to() != null) ps.add(cb.lessThan(root.<Instant>get("occurredAt"), f.to()));
            if (f.referenceType() != null) ps.add(cb.equal(root.get("referenceType"), f.referenceType()));
            if (f.referenceId() != null) ps.add(cb.equal(root.get("referenceId"), f.referenceId()));
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}


package rw.afriteck.wms.foundation.specifications;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.foundation.dtos.ItemFilter;
import rw.afriteck.wms.foundation.entity.Item;

import java.util.ArrayList;
import java.util.List;

public final class ItemSpecs {
    private ItemSpecs() {}

    public static Specification<Item> from(ItemFilter f) {
        return (root, query, cb) -> {
            // Load the category in the same query (but not in the count query used for paging)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("category", JoinType.LEFT);
            }

            List<Predicate> p = new ArrayList<>();
            if (f.q() != null) {
                String like = "%" + f.q().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("barcode")), like)));
            }
            if (f.categoryId() != null)   p.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (f.trackingType() != null) p.add(cb.equal(root.get("trackingType"), f.trackingType()));
            if (f.expiryTracked() != null) p.add(cb.equal(root.get("expiryTracked"), f.expiryTracked()));
            if (f.active() != null)       p.add(cb.equal(root.get("active"), f.active()));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}

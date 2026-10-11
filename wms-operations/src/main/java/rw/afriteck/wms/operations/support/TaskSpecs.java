package rw.afriteck.wms.operations.support;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import rw.afriteck.wms.foundation.support.Codes;
import rw.afriteck.wms.operations.dtos.TaskFilter;
import rw.afriteck.wms.operations.entity.Task;

public final class TaskSpecs {
    private TaskSpecs() {}

    /** Board order: priority, then due date, then oldest first. Used when the caller sends no sort. */
    public static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("priorityRank"),
            Sort.Order.asc("dueAt"),            // PostgreSQL puts NULLs last on ASC
            Sort.Order.asc("createdAt"));

    public static Specification<Task> from(TaskFilter f) {
        Specification<Task> spec =
                (root, query, cb) -> cb.equal(root.get("warehouseId"), f.warehouseId());

        if (f.statuses() != null && !f.statuses().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(f.statuses()));
        }
        if (f.types() != null && !f.types().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("type").in(f.types()));
        }
        if (f.priority() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), f.priority()));
        }
        if (f.assigneeId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assigneeId"), f.assigneeId()));
        } else if (f.unassignedOnly()) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("assigneeId")));
        }
        if (f.referenceType() != null && !f.referenceType().isBlank()) {
            String refType = Codes.normalize(f.referenceType());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceType"), refType));
        }
        if (f.referenceId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceId"), f.referenceId()));
        }
        String q = Codes.blankToNull(f.q());
        if (q != null) {
            String pattern = Codes.likePattern(q);          // never null here
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("taskNumber")), pattern),
                    cb.like(cb.lower(root.get("referenceNumber")), pattern)));
        }
        return spec;
    }
}

package rw.afriteck.wms.inventory.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import rw.afriteck.wms.common.exception.BusinessException;

import java.util.Set;

public final class PageRules {
    public static final int MAX_PAGE_SIZE = 100;
    private PageRules() {}

    /** Caps the size, whitelists sort fields (422 INVALID_SORT), applies a default and adds id as a stable tie-breaker. */
    public static Pageable sanitize(Pageable pageable, Set<String> allowedSorts, Sort defaultSort) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = defaultSort;
        } else {
            for (Sort.Order o : sort) {
                if (!allowedSorts.contains(o.getProperty())) {
                    throw new BusinessException("INVALID_SORT", "Cannot sort by " + o.getProperty());
                }
            }
        }
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, sort.and(Sort.by("id")));
    }
}
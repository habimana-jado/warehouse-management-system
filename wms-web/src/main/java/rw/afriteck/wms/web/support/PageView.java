package rw.afriteck.wms.web.support;

import rw.afriteck.wms.common.dtos.PageResponse;

import java.util.List;
import java.util.function.Function;

public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                          long from, long to, List<Integer> pages) {

    public static <S, T> PageView<T> of(PageResponse<S> p, Function<? super S, ? extends T> mapper) {
        List<T> rows = p.content().stream().<T>map(mapper).toList();
        long from = p.totalElements() == 0 ? 0 : (long) p.page() * p.size() + 1;
        long to = (long) p.page() * p.size() + rows.size();
        return new PageView<>(rows, p.page(), p.size(), p.totalElements(), p.totalPages(),
                from, to, Pager.window(p.page(), p.totalPages()));
    }
    public boolean hasPrevious() { return page > 0; }
    public boolean hasNext()     { return page + 1 < totalPages; }
}


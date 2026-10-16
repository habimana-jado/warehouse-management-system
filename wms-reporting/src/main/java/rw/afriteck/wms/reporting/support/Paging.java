package rw.afriteck.wms.reporting.support;

import rw.afriteck.wms.common.dtos.PageResponse;

import java.util.List;

public final class Paging {
    public static final int MAX_SIZE = 100;
    private Paging() {}
    public static int size(int requested) { return requested < 1 ? 50 : Math.min(requested, MAX_SIZE); }
    public static int page(int requested) { return Math.max(requested, 0); }

    public static <T> PageResponse<T> slice(List<T> all, int page, int size) {
        int s = size(size), p = page(page);
        long total = all.size();
        int from = (int) Math.min((long) p * s, total);
        int to = (int) Math.min(from + (long) s, total);
        return new PageResponse<>(List.copyOf(all.subList(from, to)), p, s, total, (int) ((total + s - 1) / s));
    }
    public static int totalPages(long total, int size) { return (int) ((total + size - 1) / size); }
}

package rw.afriteck.wms.common.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Chunks {
    private Chunks() {}
    public static final int SIZE = 1000;                    // keeps IN lists far below PostgreSQL's bind-parameter limit

    public static <T> List<List<T>> partition(Collection<T> items) {
        List<T> all = new ArrayList<>(items);
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < all.size(); i += SIZE) {
            chunks.add(all.subList(i, Math.min(i + SIZE, all.size())));
        }
        return chunks;
    }
}


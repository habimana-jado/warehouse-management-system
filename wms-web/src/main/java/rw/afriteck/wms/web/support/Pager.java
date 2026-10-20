package rw.afriteck.wms.web.support;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.IntStream;

final class Pager {
    /**
     * Page indexes to show; -1 marks a gap ("...").
     */
    static List<Integer> window(int current, int total) {
        if (total <= 7) return IntStream.range(0, total).boxed().toList();
        TreeSet<Integer> keep = new TreeSet<>(List.of(0, total - 1, current - 1, current, current + 1));
        keep.removeIf(p -> p < 0 || p >= total);
        List<Integer> out = new ArrayList<>();
        int last = -1;
        for (int p : keep) {
            if (last >= 0 && p - last > 1) out.add(-1);
            out.add(p);
            last = p;
        }
        return out;
    }
}

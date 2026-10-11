package rw.afriteck.wms.foundation.support;

import java.util.Locale;

public final class Codes {
    private Codes() {}

    public static String normalize(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    public static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
    public static String likePattern(String q) {
        return (q == null || q.isBlank()) ? "%" : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }
}

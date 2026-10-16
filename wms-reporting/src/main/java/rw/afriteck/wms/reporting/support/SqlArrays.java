package rw.afriteck.wms.reporting.support;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SqlArrays {
    private SqlArrays() {}
    /** PostgreSQL array literal; use as cast(:p as uuid[]). An empty collection gives '{}' (valid, unlike IN ()). */
    public static String uuids(Collection<UUID> ids) {
        return ids.stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
    }
}

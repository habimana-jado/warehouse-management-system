package rw.afriteck.wms.reporting.support;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class SqlTimes {
    private SqlTimes() {}
    public static OffsetDateTime of(Instant i) { return OffsetDateTime.ofInstant(i, ZoneOffset.UTC); }
}


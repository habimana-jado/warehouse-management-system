package rw.afriteck.wms.reporting.support;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "wms.reporting")
public record ReportingProperties(
        @DefaultValue("Africa/Kigali") String zone,
        @DefaultValue("RWF") String currency,
        @DefaultValue("20000") int exportMaxRows,
        @DefaultValue("30") int defaultExpiryDays) {
    public ZoneId zoneId() { return ZoneId.of(zone); }
}

package rw.afriteck.wms.web.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component("uiFormat")
public class UiFormat {
    private final ZoneId zone;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d · HH:mm", Locale.ENGLISH);

    public UiFormat(@Value("${wms.ui.zone:Africa/Kigali}") String zone) { this.zone = ZoneId.of(zone); }

    public String dateTime(Instant i) { return i == null ? "—" : DATE_TIME.withZone(zone).format(i); }
}

package rw.afriteck.wms.auth.dtos;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

public record ClientInfo(String userAgent, String ipAddress) {
    public static ClientInfo from(HttpServletRequest r) {
        String ua = r.getHeader(HttpHeaders.USER_AGENT);
        return new ClientInfo(ua == null ? null : ua.substring(0, Math.min(ua.length(), 255)),
                r.getRemoteAddr());
    }
}

package rw.afriteck.wms.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.UnauthorizedException;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_ATTR = "wms.auth.error";
    private final SecurityErrorWriter writer;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException ex) throws IOException {
        var cause = (UnauthorizedException) req.getAttribute(ERROR_ATTR);
        writer.write(res, HttpStatus.UNAUTHORIZED,
                cause != null ? cause.getCode() : "UNAUTHENTICATED",
                cause != null ? cause.getMessage() : "Authentication required");
    }
}

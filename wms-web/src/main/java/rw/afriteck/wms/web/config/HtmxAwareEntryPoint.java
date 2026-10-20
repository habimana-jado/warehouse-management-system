package rw.afriteck.wms.web.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

public class HtmxAwareEntryPoint implements AuthenticationEntryPoint {

    private final String loginUrl;
    private final AuthenticationEntryPoint redirect;

    public HtmxAwareEntryPoint(String loginUrl) {
        this.loginUrl = loginUrl;
        this.redirect = new LoginUrlAuthenticationEntryPoint(loginUrl);
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException, ServletException {
        if ("true".equals(req.getHeader("HX-Request"))) {
            res.setHeader("HX-Redirect", req.getContextPath() + loginUrl);   // not a login page inside a fragment
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            redirect.commence(req, res, ex);
        }
    }
}

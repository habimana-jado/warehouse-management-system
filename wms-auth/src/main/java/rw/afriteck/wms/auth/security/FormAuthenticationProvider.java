package rw.afriteck.wms.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.auth.service.AuthService;
import rw.afriteck.wms.common.exception.UnauthorizedException;

@Component
@RequiredArgsConstructor
public class FormAuthenticationProvider implements AuthenticationProvider {

    private final AuthService authService;

    @Override
    public Authentication authenticate(Authentication auth) {
        try {
            AuthenticatedUser user = authService.authenticate(auth.getName(), String.valueOf(auth.getCredentials()));
            var authorities = user.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
            return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
        } catch (UnauthorizedException e) {
            throw new BadCredentialsException(e.getCode(), e);
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(type);
    }
}

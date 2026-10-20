package rw.afriteck.wms.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import rw.afriteck.wms.auth.security.FormAuthenticationProvider;

@Configuration
public class WebSecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http, FormAuthenticationProvider provider) throws Exception {
        return http
                .authenticationProvider(provider)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/error", "/actuator/health/**",
                                "/assets/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", false)
                        .failureUrl("/login?error").permitAll())
                .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
                .exceptionHandling(e -> e.authenticationEntryPoint(new HtmxAwareEntryPoint("/login")))
                .build();                                   // CSRF stays enabled here
    }
}


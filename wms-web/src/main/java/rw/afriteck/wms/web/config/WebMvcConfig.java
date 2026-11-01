package rw.afriteck.wms.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import rw.afriteck.wms.web.support.NavInterceptor;

@Configuration
@RequiredArgsConstructor
class WebMvcConfig implements WebMvcConfigurer {

    private final NavInterceptor navInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                res.addHeader("Vary", "HX-Request");
                return true;
            }
        });

        registry.addInterceptor(navInterceptor)
                .excludePathPatterns("/assets/**", "/error", "/login");
    }
}

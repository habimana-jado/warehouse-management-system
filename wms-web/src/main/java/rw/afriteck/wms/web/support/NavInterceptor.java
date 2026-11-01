package rw.afriteck.wms.web.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UrlPathHelper;

@Component
@RequiredArgsConstructor
public class NavInterceptor implements HandlerInterceptor {

    public static final String ATTRIBUTE = "nav";

    private final NavRegistry navRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // only controller methods, and only full-page requests (htmx fragments have no shell)
        if (!(handler instanceof HandlerMethod) || "true".equals(request.getHeader("HX-Request"))) {
            return true;
        }
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);   // no context path, no ;jsessionid
        navRegistry.view(path).ifPresent(view -> request.setAttribute(ATTRIBUTE, view));
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        Object view = request.getAttribute(ATTRIBUTE);
        if (view == null || modelAndView == null) return;

        String name = modelAndView.getViewName();
        if (name == null || name.startsWith("redirect:") || name.startsWith("forward:")) return;

        if (!modelAndView.getModel().containsKey(ATTRIBUTE)) {                    // never overwrite a controller's own value
            modelAndView.addObject(ATTRIBUTE, view);
        }
    }
}
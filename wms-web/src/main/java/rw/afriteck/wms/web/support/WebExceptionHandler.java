package rw.afriteck.wms.web.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.exception.NotFoundException;

@ControllerAdvice(basePackages = "rw.afriteck.wms.web")
public class WebExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    String notFound(NotFoundException e, HttpServletResponse res, Model model) {
        res.setStatus(404);
        model.addAttribute("message", e.getMessage());
        return "error/404";
    }

    @ExceptionHandler(ForbiddenException.class)
    String forbidden(ForbiddenException e, HttpServletResponse res, Model model) {
        res.setStatus(403);
        model.addAttribute("message", e.getMessage());
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)           // the closest handler wins, so NotFound and Forbidden stay above
    String business(BusinessException e, HttpServletRequest req, HttpServletResponse res, Model model) {
        model.addAttribute("message", e.getMessage());
        if (Htmx.isRequest(req)) {
            res.setHeader("HX-Retarget", "#toast-area");
            res.setHeader("HX-Reswap", "beforeend");
            return "templates/fragments/toast :: error";
        }
        res.setStatus(422);
        return "error/business";
    }
}

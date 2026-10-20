package rw.afriteck.wms.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.net.URI;
import java.util.UUID;

@Controller
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseSelectController {

    private final WarehouseContext warehouseContext;

    @PostMapping("/select")
    String select(@RequestParam UUID warehouseId,
                  @RequestHeader(value = "Referer", required = false) String referer,
                  HttpServletRequest request) {
        warehouseContext.select(warehouseId);
        return "redirect:" + listPageOf(referer, request);
    }

    /** Back to the list page the user came from (first path segment), never to an arbitrary URL. */
    private static String listPageOf(String referer, HttpServletRequest request) {
        if (referer == null) return "/";
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) return "/";
            String path = uri.getPath() == null ? "/" : uri.getPath();
            String ctx = request.getContextPath();
            if (!ctx.isEmpty() && path.startsWith(ctx)) path = path.substring(ctx.length());
            String[] parts = path.split("/");
            return parts.length > 1 && parts[1].matches("[A-Za-z0-9_-]+") ? "/" + parts[1] : "/";
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}

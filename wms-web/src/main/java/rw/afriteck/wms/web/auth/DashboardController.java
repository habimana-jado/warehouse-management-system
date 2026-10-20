package rw.afriteck.wms.web.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    String home() {
        return "dashboard/index";     // templates/dashboard/index.html
    }
}

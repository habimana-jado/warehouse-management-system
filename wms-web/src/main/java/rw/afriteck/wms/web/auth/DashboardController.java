package rw.afriteck.wms.web.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("launcher", true);               // layout/base collapses the sidebar
        return "dashboard/modules";
    }
}

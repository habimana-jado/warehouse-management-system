package rw.afriteck.wms.web.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.web.config.WebAccess;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
class ItemLookupController {

    private final ItemService itemService;

    /** htmx: the <option> list of the shared datalist. The typed text is the only parameter (its name depends on the field). */
    @GetMapping("/lookup/items")
    @PreAuthorize(WebAccess.ANY_USER)
    String options(@RequestParam Map<String, String> params, Model model) {
        String q = params.values().stream().filter(StringUtils::hasText).findFirst().orElse("").trim();
        model.addAttribute("items", q.length() < 2 ? List.of() : itemService.searchForPicker(q, 10));
        return "inventory/fragments :: itemOptions";
    }
}

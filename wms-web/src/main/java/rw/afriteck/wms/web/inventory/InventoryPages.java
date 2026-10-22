package rw.afriteck.wms.web.inventory;

import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class InventoryPages {

    static final List<Integer> PAGE_SIZES = List.of(15, 25, 50, 100);

    private InventoryPages() {}

    /** Puts sortUrl and sortClass (maps field -> url / css class) into the model; both rebuild the current URL. */
    static void sortLinks(Model model, UiLinks uiLinks, String... fields) {
        Map<String, String> urls = new HashMap<>();
        Map<String, String> classes = new HashMap<>();
        for (String field : fields) {
            urls.put(field, uiLinks.sort(field));
            classes.put(field, uiLinks.sortClass(field));
        }
        model.addAttribute("sortUrl", urls);
        model.addAttribute("sortClass", classes);
    }

    static <T> PageView<T> empty(Pageable pageable) {
        PageResponse<T> none = new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
        return PageView.of(none, Function.identity());
    }
}

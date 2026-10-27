package rw.afriteck.wms.web.outbound;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.outbound.dtos.PickListSummaryResponse;
import rw.afriteck.wms.outbound.enums.PickListStatus;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.UiLinks;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.*;

@Controller
@RequestMapping("/pick-lists")
@RequiredArgsConstructor
class PickListPageController {

    private static final List<String> SORT_FIELDS = List.of("releasedAt", "pickNumber", "status");   // match PickListServiceImpl

    private final PickListService pickLists;
    private final WarehouseContext warehouseContext;
    private final UiLinks uiLinks;
    private final PickListViews views;

    @ModelAttribute("statuses")
    List<PickListStatus> statuses() {
        return List.of(PickListStatus.values());
    }

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return List.of(15, 25, 50, 100);
    }

    @GetMapping
    @PreAuthorize(WebAccess.BOARD)
    String list(@ModelAttribute("filter") PickListFilterForm filter,
                @PageableDefault(size = 15, sort = "releasedAt", direction = Sort.Direction.DESC) Pageable pageable,
                Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<PickListSummaryResponse> response = pickLists.search(warehouseId, boardStatuses(filter.getStatus()), pageable);
        model.addAttribute("page", PageView.of(response, s -> s));
        Map<String, String> urls = new LinkedHashMap<>();
        Map<String, String> classes = new LinkedHashMap<>();
        for (String field : SORT_FIELDS) {
            urls.put(field, uiLinks.sort(field));
            classes.put(field, uiLinks.sortClass(field));
        }
        model.addAttribute("sortUrl", urls);
        model.addAttribute("sortClass", classes);
        return Htmx.isFragmentRequest(request) ? "outbound/pick-lists :: results" : "outbound/pick-lists";
    }

    @GetMapping("/{id}")
    @PreAuthorize(WebAccess.OUTBOUND_PICK_READ)
    String detail(@PathVariable UUID id, Model model, HttpServletRequest request) {
        views.fill(model, id);
        return Htmx.isFragmentRequest(request) ? "outbound/pick-list-detail :: panel" : "outbound/pick-list-detail";
    }

    /** blank = open lists, ALL = no filter (null), otherwise one status; an unknown value falls back to open lists. */
    private static Set<PickListStatus> boardStatuses(String raw) {
        String value = Codes.blankToNull(raw);
        if (value == null) {
            return PickListStatus.OPEN;
        }
        if ("ALL".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return EnumSet.of(PickListStatus.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return PickListStatus.OPEN;
        }
    }
}

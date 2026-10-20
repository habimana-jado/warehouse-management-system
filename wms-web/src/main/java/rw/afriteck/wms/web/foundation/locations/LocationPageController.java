package rw.afriteck.wms.web.foundation.locations;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.LocationTreeResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
@PreAuthorize(WebAccess.BOARD)                       // reads; the write methods below tighten it to MANAGER
public class LocationPageController {

    private static final Set<String> SORTS = Set.of("code", "name");

    private final LocationService locations;
    private final WarehouseContext warehouseContext;
    private final UserContext userContext;
    private final UiLinks uiLinks;

    @ModelAttribute("types")
    List<LocationType> types()   { return List.of(LocationType.values()); }
    @ModelAttribute("usages") List<LocationUsage> usages() { return List.of(LocationUsage.values()); }

    // ---------- list and tree ----------

    @GetMapping
    String list(@RequestParam(required = false) String q,
                @PageableDefault(size = 15) Pageable pageable,
                HttpServletRequest request, Model model) {

        WarehouseOption wh = warehouseContext.requireCurrent();
        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by("code"));       // adjust to the real signature
        PageResponse<LocationResponse> result = PageResponse.of(locations.search(wh.id(), q, safe));
        Map<UUID, String> parentCodes = parentCodes(result.content());

        model.addAttribute("page", PageView.of(result, l -> toRow(l, parentCodes)));
        model.addAttribute("q", q);
        model.addAttribute("sorts", uiLinks.sorts("code", "name"));
        model.addAttribute("warehouseLabel", wh.code() + " · " + wh.name());
        model.addAttribute("canManage", canManage());
        return Htmx.isFragmentRequest(request) ? "locations/list :: results" : "locations/list";
    }

    @GetMapping("/tree")
    String tree(Model model) {
        WarehouseOption wh = warehouseContext.requireCurrent();
        List<LocationTreeRow> rows = new ArrayList<>();
        flatten(locations.tree(wh.id()), 0, rows);
        model.addAttribute("rows", rows);
        model.addAttribute("warehouseLabel", wh.code() + " · " + wh.name());
        return "locations/tree";
    }

    // ---------- create ----------

    @GetMapping("/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        model.addAttribute("form", new LocationCreateForm());
        addCreateLookups(model, warehouseContext.requireCurrent());
        return "locations/create";
    }

    @PostMapping
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") LocationCreateForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        WarehouseOption wh = warehouseContext.requireCurrent();
        if (!result.hasErrors()) {
            try {
                LocationResponse created = locations.create(form.toRequest(wh.id()));
                redirect.addFlashAttribute("flash", Flash.success("locations.created", created.code()));
                return "redirect:/locations";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of(
                        "LOCATION_CODE_EXISTS", "code",
                        "LOCATION_BARCODE_EXISTS", "barcode",
                        "INVALID_HIERARCHY", "type",
                        "INVALID_USAGE", "usage",
                        "PARENT_INACTIVE", "parentId",
                        "PARENT_OTHER_WAREHOUSE", "parentId"));
            }
        }
        addCreateLookups(model, wh);
        return "locations/create";                                                    // status 200 on purpose
    }

    // ---------- edit ----------

    @GetMapping("/{id}/edit")
    @PreAuthorize(WebAccess.MANAGER)
    String edit(@PathVariable UUID id, Model model) {
        LocationResponse l = load(id);
        fillEditModel(model, l);
        model.addAttribute("form", LocationEditForm.from(l));
        return "locations/edit";
    }

    @PostMapping("/{id}/details")
    @PreAuthorize(WebAccess.MANAGER)
    String updateDetails(@PathVariable UUID id, @Valid @ModelAttribute("form") LocationEditForm form,
                         BindingResult result, Model model) {
        fillEditModel(model, load(id));
        if (!result.hasErrors()) {
            try {
                locations.update(id, form.toRequest());
                model.addAttribute("saved", true);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("LOCATION_BARCODE_EXISTS", "barcode", "INVALID_USAGE", "usage"));
            }
        }
        return "locations/edit :: detailsCard";
    }

    // ---------- row actions (conflicts such as active children or stock reach the user as a toast) ----------

    @PostMapping("/{id}/activate")
    @PreAuthorize(WebAccess.MANAGER)
    String activate(@PathVariable UUID id, Model model) {
        load(id);                                                                      // warehouse access check
        return row(locations.activate(id), model);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize(WebAccess.MANAGER)
    String deactivate(@PathVariable UUID id, Model model) {
        load(id);
        return row(locations.deactivate(id), model);
    }

    // ---------- helpers ----------

    /** Loads a location and checks the user may work in its warehouse. */
    private LocationResponse load(UUID id) {
        LocationResponse l = locations.getById(id);
        userContext.requireWarehouseAccess(l.warehouseId());
        return l;
    }

    private String row(LocationResponse l, Model model) {
        model.addAttribute("loc", toRow(l, parentCodes(List.of(l))));
        model.addAttribute("canManage", true);
        return "locations/list :: row";
    }

    private boolean canManage() {
        return userContext.hasRole("ADMIN") || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private Map<UUID, String> parentCodes(List<LocationResponse> items) {
        Set<UUID> ids = items.stream().map(LocationResponse::parentId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return locations.findAllByIds(ids).stream()
                .collect(Collectors.toMap(LocationResponse::id, LocationResponse::code));
    }

    private LocationRow toRow(LocationResponse l, Map<UUID, String> parentCodes) {
        return new LocationRow(l.id(), l.code(), l.name(), l.type(), l.usage(),
                l.parentId() == null ? null : parentCodes.get(l.parentId()), l.barcode(), l.active());
    }

    private void flatten(List<LocationTreeResponse> nodes, int depth, List<LocationTreeRow> out) {
        for (LocationTreeResponse n : nodes) {
            out.add(new LocationTreeRow(depth, n));
            flatten(n.children(), depth + 1, out);
        }
    }

    private void addCreateLookups(Model model, WarehouseOption wh) {
        List<ParentOption> options = new ArrayList<>();
        collectParents(locations.tree(wh.id()), 0, options);
        model.addAttribute("parentOptions", options);
        model.addAttribute("warehouseLabel", wh.code() + " · " + wh.name());
    }

    /** Active, non-bin locations (a bin cannot have children), indented by depth. */
    private void collectParents(List<LocationTreeResponse> nodes, int depth, List<ParentOption> out) {
        for (LocationTreeResponse n : nodes) {
            if (n.active() && n.type() != LocationType.BIN) {
                String label = "— ".repeat(depth) + n.code() + (n.name() != null ? " · " + n.name() : "");
                out.add(new ParentOption(n.id(), label));
            }
            collectParents(n.children(), depth + 1, out);
        }
    }

    private void fillEditModel(Model model, LocationResponse l) {
        model.addAttribute("locationId", l.id());
        model.addAttribute("code", l.code());
        model.addAttribute("type", l.type());
        model.addAttribute("isBin", l.type() == LocationType.BIN);
        model.addAttribute("parentCode", l.parentId() == null ? null : locations.getById(l.parentId()).code());
    }
}
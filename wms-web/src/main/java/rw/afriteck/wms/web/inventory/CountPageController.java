package rw.afriteck.wms.web.inventory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.CountFilter;
import rw.afriteck.wms.inventory.dtos.CountLineResponse;
import rw.afriteck.wms.inventory.dtos.CountResponse;
import rw.afriteck.wms.inventory.dtos.RecordCountRequest;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.service.CountService;
import rw.afriteck.wms.inventory.support.InventoryReferences;
import rw.afriteck.wms.operations.dtos.TaskFilter;
import rw.afriteck.wms.operations.dtos.TaskSummaryResponse;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
class CountPageController {

    private static final String PANEL = "inventory/count-detail :: panel";
    private static final String PAGE = "inventory/count-detail";
    private static final String LINES_CHANGED = "lines-changed";

    private static final Map<String, String> ADD_ERROR_FIELDS = Map.of(
            "LOT_NOT_FOUND", "lotNumber",
            "LOCATION_WRONG_WAREHOUSE", "locationId",
            "COUNT_LOCATION_OUT_OF_SCOPE", "locationId",
            "QTY_INVALID", "countedQty");

    private final CountService counts;
    private final TaskService taskService;
    private final WarehouseService warehouseService;
    private final LocationService locationService;
    private final WarehouseContext warehouseContext;
    private final UserContext userContext;
    private final BinOptions binOptions;
    private final ZoneOptions zoneOptions;
    private final ItemResolver itemResolver;
    private final UiLinks uiLinks;

    @Value("${wms.ui.zone:Africa/Kigali}")
    private String zone;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    @ModelAttribute("statuses")
    List<CountStatus> statuses() {
        return List.of(CountStatus.values());
    }

    @ModelAttribute("priorities")
    List<TaskPriority> priorities() {
        return List.of(TaskPriority.values());
    }

    // ================================================================== board

    @GetMapping("/counts")
    @PreAuthorize(WebAccess.BOARD)
    String board(@ModelAttribute("filter") CountFilterForm filter, BindingResult binding,
                 @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        Set<CountStatus> statuses = filter.getStatus() == null ? Set.of() : EnumSet.of(filter.getStatus());
        PageResponse<CountResponse> result = counts.search(new CountFilter(warehouseId, statuses, filter.getQ()), pageable);

        Set<UUID> zoneIds = result.content().stream().map(CountResponse::zoneLocationId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> zoneNames = zoneIds.isEmpty() ? Map.of() : locationService.findAllByIds(zoneIds).stream()
                .collect(Collectors.toMap(LocationResponse::id, LocationResponse::code));
        model.addAttribute("page", PageView.of(result,
                c -> new CountRow(c, c.zoneLocationId() == null ? null : zoneNames.get(c.zoneLocationId()))));
        InventoryPages.sortLinks(model, uiLinks, "createdAt", "countNumber", "status");
        return Htmx.isFragmentRequest(request) ? "inventory/counts :: results" : "inventory/counts";
    }

    // ================================================================== create

    @GetMapping("/counts/new")
    @PreAuthorize(WebAccess.MANAGER)
    String newForm(Model model) {
        model.addAttribute("form", new CountForm());
        fillForm(model);
        return "inventory/count-form";
    }

    @PostMapping("/counts")
    @PreAuthorize(WebAccess.MANAGER)
    String create(@Valid @ModelAttribute("form") CountForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        if (!result.hasErrors()) {
            try {
                CountResponse created = counts.create(form.toRequest(warehouseId));
                redirect.addFlashAttribute("flash", Flash.success("inventory.count.flash.created", created.countNumber()));
                return "redirect:/counts/" + created.id();
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("ZONE_INVALID", "zoneLocationId"));
            }
        }
        fillForm(model);
        return "inventory/count-form";
    }

    private void fillForm(Model model) {
        model.addAttribute("zones", zoneOptions.forWarehouse(warehouseContext.require()));
        model.addAttribute("menu", "counts");
    }

    // ================================================================== detail

    @GetMapping("/counts/{id}")
    @PreAuthorize(WebAccess.ANY_USER)
    String detail(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean onlyVariance,
                  @PageableDefault(size = 50) Pageable pageable, Model model) {
        requireAccess(id);
        CountResponse count = counts.getById(id);
        fillPanel(model, count);
        fillLines(model, count, onlyVariance, pageable);
        return PAGE;
    }

    /** htmx: the lines card (paging, "only differences", and the refresh after an action). */
    @GetMapping("/counts/{id}/lines")
    @PreAuthorize(WebAccess.ANY_USER)
    String lines(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean onlyVariance,
                 @PageableDefault(size = 50) Pageable pageable, HttpServletRequest request, Model model) {
        if (!Htmx.isFragmentRequest(request)) {
            return "redirect:/counts/" + id;
        }
        requireAccess(id);
        CountResponse count = counts.getById(id);
        model.addAttribute("canCount", isWorker() && count.status() == CountStatus.IN_PROGRESS);
        fillLines(model, count, onlyVariance, pageable);
        return "inventory/count-detail :: linesCard";
    }

    // ================================================================== record a count (one row)

    @PostMapping("/counts/{id}/lines/{lineId}/count")
    @PreAuthorize(WebAccess.WORKER)
    String record(@PathVariable UUID id, @PathVariable UUID lineId, @RequestParam(required = false) BigDecimal countedQty,
                  HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirect) {
        requireAccess(id);
        boolean htmx = Htmx.isFragmentRequest(request);
        try {
            CountLineResponse line = counts.recordCount(id, lineId, new RecordCountRequest(countedQty),
                    userContext.currentUserId(), isManager());
            if (!htmx) {
                return "redirect:/counts/" + id;
            }
            model.addAttribute("l", line);
            model.addAttribute("countId", id);
            model.addAttribute("canCount", true);
            model.addAttribute("count", counts.getById(id));            // fresh progress numbers for the panel
            return "inventory/count-detail :: rowAndProgress";
        } catch (BusinessException e) {   // QTY_INVALID, COUNT_TASK_NOT_STARTED, task assigned to someone else
            if (!htmx) {
                redirect.addFlashAttribute("error", e.getMessage());
                return "redirect:/counts/" + id;
            }
            response.setHeader("HX-Reswap", "none");                    // keep the row; only the alert above the table changes
            model.addAttribute("message", e.getMessage());
            return "inventory/count-detail :: rowError";
        }
    }

    // ================================================================== found stock (add a line)

    @PostMapping("/counts/{id}/lines")
    @PreAuthorize(WebAccess.WORKER)
    String addLine(@PathVariable UUID id, @Valid @ModelAttribute("addForm") AddCountLineForm form, BindingResult result,
                   HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirect) {
        requireAccess(id);
        boolean htmx = Htmx.isFragmentRequest(request);

        ItemResolver.Resolution match = itemResolver.resolve(form.getItem());
        if (!match.found() && !result.hasFieldErrors("item")) {
            if (match.ambiguous()) {
                result.rejectValue("item", "inventory.item.ambiguous", new Object[]{match.candidateSkus()}, "Several items match");
            } else {
                result.rejectValue("item", "inventory.item.unknown", new Object[]{form.getItem()}, "Unknown item");
            }
        }
        if (!result.hasErrors()) {
            try {
                counts.addLine(id, form.toRequest(match.item().id()), userContext.currentUserId());
                if (!htmx) {
                    redirect.addFlashAttribute("flash", Flash.success("inventory.count.flash.lineAdded"));
                    return "redirect:/counts/" + id;
                }
                response.setHeader("HX-Trigger", LINES_CHANGED);        // the lines card reloads and shows the new line
                model.addAttribute("addForm", new AddCountLineForm());
                model.addAttribute("addNotice", true);
                fillAddCard(model);
                return "inventory/count-detail :: addCard";
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ADD_ERROR_FIELDS);
            } catch (DataIntegrityViolationException e) {               // the same bin, item, lot and serial is already in this count
                result.reject("inventory.count.duplicateLine", "This stock is already a line of the count");
            }
        }
        if (htmx) {
            fillAddCard(model);
            return "inventory/count-detail :: addCard";
        }
        CountResponse count = counts.getById(id);                       // plain post with errors: draw the whole page
        fillPanel(model, count);
        fillLines(model, count, false, PageRequest.of(0, 50));
        return PAGE;
    }

    // ================================================================== panel actions

    @PostMapping("/counts/{id}/start")
    @PreAuthorize(WebAccess.MANAGER)
    String start(@PathVariable UUID id, @ModelAttribute("startForm") StartCountForm form, BindingResult ignored,
                 HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirect) {
        return run(id, "inventory.count.flash.started", () -> counts.start(id, form.toRequest(ZoneId.of(zone))),
                request, response, model, redirect);
    }

    @PostMapping("/counts/{id}/submit")
    @PreAuthorize(WebAccess.MANAGER)
    String submit(@PathVariable UUID id, HttpServletRequest request, HttpServletResponse response,
                  Model model, RedirectAttributes redirect) {
        return run(id, "inventory.count.flash.submitted", () -> counts.submit(id), request, response, model, redirect);
    }

    @PostMapping("/counts/{id}/reopen")
    @PreAuthorize(WebAccess.MANAGER)
    String reopen(@PathVariable UUID id, HttpServletRequest request, HttpServletResponse response,
                  Model model, RedirectAttributes redirect) {
        return run(id, "inventory.count.flash.reopened", () -> counts.reopen(id), request, response, model, redirect);
    }

    @PostMapping("/counts/{id}/approve")
    @PreAuthorize(WebAccess.MANAGER)
    String approve(@PathVariable UUID id, HttpServletRequest request, HttpServletResponse response,
                   Model model, RedirectAttributes redirect) {
        return run(id, "inventory.count.flash.approved", () -> counts.approve(id, userContext.currentUserId()),
                request, response, model, redirect);
    }

    @PostMapping("/counts/{id}/cancel")
    @PreAuthorize(WebAccess.MANAGER)
    String cancel(@PathVariable UUID id, @Valid @ModelAttribute("cancelForm") CancelCountForm form, BindingResult result,
                  HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {                                       // re-render with the field error, status 200
            requireAccess(id);
            fillPanel(model, counts.getById(id));
            if (!Htmx.isFragmentRequest(request)) {
                fillLines(model, counts.getById(id), false, PageRequest.of(0, 50));
            }
            return Htmx.isFragmentRequest(request) ? PANEL : PAGE;
        }
        return run(id, "inventory.count.flash.cancelled", () -> counts.cancel(id, form.getReason()),
                request, response, model, redirect);
    }

    /** Runs one action: htmx gets the re-rendered panel (and the lines card reloads), a plain post is redirected. */
    private String run(UUID id, String messageKey, PanelAction<CountResponse> action,
                       HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirect) {
        requireAccess(id);
        boolean htmx = Htmx.isFragmentRequest(request);
        try {
            CountResponse updated = action.execute();
            if (htmx) {
                fillPanel(model, updated);
                model.addAttribute("notice", messageKey);
                response.setHeader("HX-Trigger", LINES_CHANGED);
                return PANEL;
            }
            redirect.addFlashAttribute("flash", Flash.success(messageKey, updated.countNumber()));
            return "redirect:/counts/" + id;
        } catch (BusinessException e) {   // e.g. COUNT_LINES_PENDING, COUNT_ALREADY_ACTIVE, INSUFFICIENT_STOCK
            if (htmx) {
                fillPanel(model, counts.getById(id));
                model.addAttribute("error", e.getMessage());
                return PANEL;
            }
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/counts/" + id;
        }
    }

    // ================================================================== helpers

    private void fillPanel(Model model, CountResponse c) {
        boolean manager = isManager();
        boolean worker = isWorker();
        CountStatus s = c.status();
        List<TaskSummaryResponse> tasks = s == CountStatus.DRAFT ? List.of() : taskService.search(
                new TaskFilter(c.warehouseId(), EnumSet.allOf(TaskStatus.class), EnumSet.allOf(TaskType.class),
                        null, null, false, InventoryReferences.STOCK_COUNT, c.id(), null),
                PageRequest.of(0, 100, Sort.by("taskNumber"))).content();

        boolean canAddFound = worker && s == CountStatus.IN_PROGRESS;
        model.addAttribute("view", new CountView(c,
                warehouseService.getById(c.warehouseId()).name(),
                c.zoneLocationId() == null ? null : locationService.getById(c.zoneLocationId()).code(),
                tasks,
                manager && s == CountStatus.DRAFT,
                manager && s == CountStatus.IN_PROGRESS,
                manager && s == CountStatus.REVIEW,
                manager && s == CountStatus.REVIEW,
                manager && (s == CountStatus.DRAFT || s == CountStatus.IN_PROGRESS || s == CountStatus.REVIEW),
                worker && s == CountStatus.IN_PROGRESS,
                canAddFound));
        model.addAttribute("canCount", worker && s == CountStatus.IN_PROGRESS);
        if (!model.containsAttribute("startForm")) {
            model.addAttribute("startForm", new StartCountForm());
        }
        if (!model.containsAttribute("cancelForm")) {                   // keep the bound form when it carries errors
            model.addAttribute("cancelForm", new CancelCountForm());
        }
        if (canAddFound) {
            if (!model.containsAttribute("addForm")) {
                model.addAttribute("addForm", new AddCountLineForm());
            }
            fillAddCard(model);
        }
        model.addAttribute("menu", "counts");
    }

    private void fillAddCard(Model model) {
        model.addAttribute("bins", binOptions.forWarehouse(warehouseContext.require()));
    }

    private void fillLines(Model model, CountResponse count, boolean onlyVariance, Pageable pageable) {
        model.addAttribute("linesPage", PageView.of(counts.lines(count.id(), onlyVariance, pageable), Function.identity()));
        model.addAttribute("onlyVariance", onlyVariance);
        model.addAttribute("countId", count.id());
        model.addAttribute("count", count);
    }

    private void requireAccess(UUID id) {
        userContext.requireWarehouseAccess(counts.warehouseIdOf(id));
    }

    private boolean isManager() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private boolean isWorker() {
        return isManager() || userContext.hasRole("RECEIVER") || userContext.hasRole("PICKER");
    }
}

package rw.afriteck.wms.web.operations;

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
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.operations.dtos.TaskAccessInfo;
import rw.afriteck.wms.operations.dtos.TaskFilter;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.dtos.TaskSummaryResponse;
import rw.afriteck.wms.operations.enums.TaskLineStatus;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.operations.support.ClaimableTaskTypes;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskPageController {

    private static final String LIST = "operations/tasks";
    private static final String DETAIL = "operations/task-detail";
    private static final String PANEL = DETAIL + " :: taskPanel";
    private static final Set<String> SORTABLE = Set.of("priorityRank", "dueAt", "createdAt", "taskNumber", "status");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("priorityRank"),
            Sort.Order.asc("dueAt"), Sort.Order.asc("createdAt"));

    private final TaskService taskService;
    private final UserContext userContext;
    private final WarehouseContext warehouseContext;
    private final WorkerLookup workerLookup;
    private final TaskReferenceLinks links;
    private final Clock clock;
    private final UiLinks uiLinks;

    @ModelAttribute("statuses")
    TaskStatus[]   statuses()   { return TaskStatus.values(); }
    @ModelAttribute("types")
    TaskType[]     types()      { return TaskType.values(); }
    @ModelAttribute("priorities")
    TaskPriority[] priorities() { return TaskPriority.values(); }
    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() { return List.of(15, 25, 50, 100); }
    // ------------------------------------------------------------ lists

    /** Board: managers, admins and viewers see every task of the selected warehouse. */
    @GetMapping
    @PreAuthorize(WebAccess.BOARD)
    public String board(@ModelAttribute("filter") TaskFilterForm filter,
                        @PageableDefault(size = 15) Pageable pageable,
                        Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        return list(filter.toFilter(warehouseId, null), pageable, false, model, request);
    }

    /** The caller's own tasks. */
    @GetMapping("/my")
    @PreAuthorize(WebAccess.WORKER)
    public String myTasks(@ModelAttribute("filter") TaskFilterForm filter,
                          @PageableDefault(size = 15) Pageable pageable,
                          Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        return list(filter.toFilter(warehouseId, userContext.currentUserId()), pageable, true, model, request);
    }

    private String list(TaskFilter filter, Pageable pageable, boolean mine, Model model, HttpServletRequest request) {
        Pageable p = PageRules.sanitize(pageable, SORTABLE, DEFAULT_SORT);
        PageView<TaskRow> page = PageView.of(taskService.search(filter, p), s -> toRow(s, mine));
        model.addAttribute("page", page);
        model.addAttribute("mine", mine);
        model.addAttribute("basePath", mine ? "/tasks/my" : "/tasks");
        model.addAttribute("menu", mine ? "my-tasks" : "tasks");
        addSortLinks(model);
        return Htmx.isFragmentRequest(request) ? LIST + " :: results" : LIST;
    }

    // ------------------------------------------------------------ claim next

    @PostMapping("/claim-next")
    @PreAuthorize(WebAccess.WORKER)
    public String claimNext(RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        Set<TaskType> claimable = ClaimableTaskTypes.forUser(userContext);
        Optional<TaskResponse> claimed = claimable.isEmpty() ? Optional.empty()
                : taskService.claimNext(warehouseId, claimable, userContext.currentUserId());
        if (claimed.isPresent()) {
            redirect.addFlashAttribute("flash", Flash.success("operations.flash.claimed", claimed.get().taskNumber()));
            return "redirect:/tasks/" + claimed.get().id();
        }
        redirect.addFlashAttribute("flash", Flash.success("operations.flash.claim.none"));
        return "redirect:/tasks/my";
    }

    // ------------------------------------------------------------ detail

    @GetMapping("/{id}")
    @PreAuthorize(WebAccess.ANY_USER)
    public String detail(@PathVariable UUID id, Model model) {
        requireReadAccess(authorize(id));
        populate(model, id);
        return DETAIL;
    }

    @PostMapping("/{id}/start")
    @PreAuthorize(WebAccess.WORKER)
    public String start(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean row,
                        Model model, HttpServletRequest request, RedirectAttributes redirect) {
        requireAssigneeOrManager(authorize(id));
        if (row) {                                   // "Start" button inside a list row
            taskService.start(id);
            model.addAttribute("t", toRow(taskService.getById(id)));
            model.addAttribute("mine", true);
            return LIST + " :: row";
        }
        return act(id, "operations.flash.started", () -> taskService.start(id), model, request, redirect);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize(WebAccess.WORKER)
    public String release(@PathVariable UUID id, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        requireAssigneeOrManager(authorize(id));
        return act(id, "operations.flash.released", () -> taskService.release(id), model, request, redirect);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize(WebAccess.MANAGER)
    public String assign(@PathVariable UUID id,
                         @Valid @ModelAttribute("assignForm") AssignTaskForm form, BindingResult result,
                         Model model, HttpServletRequest request, RedirectAttributes redirect) {
        authorize(id);
        if (!result.hasErrors()) {
            try {
                return act(id, "operations.flash.assigned",
                        () -> taskService.assign(id, form.toRequest().assigneeId()), model, request, redirect);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("ASSIGNEE_NOT_ALLOWED", "assigneeId"));
            }
        }
        populate(model, id);
        return Htmx.isFragmentRequest(request) ? PANEL : DETAIL;
    }

    @PostMapping("/{id}/priority")
    @PreAuthorize(WebAccess.MANAGER)
    public String changePriority(@PathVariable UUID id,
                                 @Valid @ModelAttribute("priorityForm") ChangePriorityForm form, BindingResult result,
                                 Model model, HttpServletRequest request, RedirectAttributes redirect) {
        authorize(id);
        if (!result.hasErrors()) {
            try {
                return act(id, "operations.flash.priority",
                        () -> taskService.changePriority(id, form.toRequest().priority()), model, request, redirect);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of());
            }
        }
        populate(model, id);
        return Htmx.isFragmentRequest(request) ? PANEL : DETAIL;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(WebAccess.MANAGER)
    public String cancel(@PathVariable UUID id,
                         @Valid @ModelAttribute("cancelForm") CancelTaskForm form, BindingResult result,
                         Model model, HttpServletRequest request, RedirectAttributes redirect) {
        authorize(id);
        if (!result.hasErrors()) {
            try {
                return act(id, "operations.flash.cancelled",
                        () -> taskService.cancel(id, form.toRequest().reason()), model, request, redirect);
            } catch (BusinessException e) {
                FormErrors.apply(result, e, Map.of("CANCEL_REASON_REQUIRED", "reason"));
            }
        }
        populate(model, id);
        return Htmx.isFragmentRequest(request) ? PANEL : DETAIL;
    }

    // ------------------------------------------------------------ helpers: actions

    /**
     * Runs a state change. htmx: re-render the whole panel with a notice; plain post: flash + redirect.
     * A 409 (task changed under the user) is shown inside the panel instead of an error page.
     * BusinessException is NOT caught here: forms map it to a field, other callers get the global toast.
     */
    private String act(UUID id, String messageKey, Runnable action, Model model,
                       HttpServletRequest request, RedirectAttributes redirect) {
        try {
            action.run();
        } catch (ConflictException e) {
            model.addAttribute("error", e.getMessage());
            populate(model, id);
            return Htmx.isFragmentRequest(request) ? PANEL : DETAIL;
        }
        if (Htmx.isFragmentRequest(request)) {
            populate(model, id);
            model.addAttribute("notice", messageKey);
            return PANEL;
        }
        redirect.addFlashAttribute("flash", Flash.success(messageKey));
        return "redirect:/tasks/" + id;
    }

    /** Loads everything the detail page and the panel fragment need; keeps forms that already carry errors. */
    private void populate(Model model, UUID id) {
        TaskResponse task = taskService.getById(id);
        TaskAccessInfo info = new TaskAccessInfo(task.warehouseId(), task.assigneeId());
        boolean manager = isManager();
        boolean mineOrManager = manager || isAssignee(info);
        TaskStatus s = task.status();

        TaskView view = new TaskView(task,
                links.urlFor(task.referenceType(), task.referenceId()),
                isOverdue(s, task.dueAt()),
                s == TaskStatus.ASSIGNED && mineOrManager,
                (s == TaskStatus.ASSIGNED || s == TaskStatus.IN_PROGRESS) && mineOrManager,
                manager && (s == TaskStatus.OPEN || s == TaskStatus.ASSIGNED),
                manager && TaskStatus.ACTIVE.contains(s),
                manager && TaskStatus.ACTIVE.contains(s),
                taskService.history(id));
        model.addAttribute("view", view);
        model.addAttribute("menu", manager || userContext.hasRole("VIEWER") ? "tasks" : "my-tasks");

        if (view.canAssign()) {
            model.addAttribute("workers", workerLookup.findAssignableWorkers(task.warehouseId()));
        }
        if (!model.containsAttribute("assignForm")) {
            model.addAttribute("assignForm", new AssignTaskForm());
        }
        if (!model.containsAttribute("priorityForm")) {
            ChangePriorityForm f = new ChangePriorityForm();
            f.setPriority(task.priority());
            model.addAttribute("priorityForm", f);
        }
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelTaskForm());
        }
    }

    // ------------------------------------------------------------ helpers: access (same rules as the REST controller)

    private TaskAccessInfo authorize(UUID taskId) {
        TaskAccessInfo info = taskService.accessInfo(taskId);
        userContext.requireWarehouseAccess(info.warehouseId());
        return info;
    }

    private boolean isManager() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private boolean isAssignee(TaskAccessInfo info) {
        return info.assigneeId() != null && info.assigneeId().equals(userContext.currentUserId());
    }

    /** Managers, admins and viewers read any task in their warehouses; workers only their own. */
    private void requireReadAccess(TaskAccessInfo info) {
        if (isManager() || userContext.hasRole("VIEWER") || isAssignee(info)) return;
        throw new ForbiddenException("This task is not assigned to you");
    }

    private void requireAssigneeOrManager(TaskAccessInfo info) {
        if (isManager() || isAssignee(info)) return;
        throw new ForbiddenException("This task is not assigned to you");
    }

    // ------------------------------------------------------------ helpers: mapping

    private boolean isOverdue(TaskStatus status, Instant dueAt) {
        return dueAt != null && TaskStatus.ACTIVE.contains(status) && dueAt.isBefore(clock.instant());
    }

    private TaskRow toRow(TaskSummaryResponse s, boolean mine) {
        return new TaskRow(s.id(), s.taskNumber(), s.type(), s.status(), s.priority(),
                s.referenceNumber(), links.urlFor(s.referenceType(), s.referenceId()), s.assigneeName(),
                s.dueAt(), isOverdue(s.status(), s.dueAt()), s.lineCount(), s.doneLineCount(),
                mine && s.status() == TaskStatus.ASSIGNED);
    }

    private TaskRow toRow(TaskResponse r) {
        int done = (int) r.lines().stream().filter(l -> l.status() != TaskLineStatus.PENDING).count();
        return new TaskRow(r.id(), r.taskNumber(), r.type(), r.status(), r.priority(),
                r.referenceNumber(), links.urlFor(r.referenceType(), r.referenceId()), r.assigneeName(),
                r.dueAt(), isOverdue(r.status(), r.dueAt()), r.lines().size(), done,
                r.status() == TaskStatus.ASSIGNED);
    }
    private void addSortLinks(Model model) {
        Map<String, String> urls = new HashMap<>();
        Map<String, String> classes = new HashMap<>();
        for (String field : List.of("taskNumber", "priorityRank", "status", "dueAt")) {
            urls.put(field, uiLinks.sort(field));
            classes.put(field, uiLinks.sortClass(field));
        }
        model.addAttribute("sortUrl", urls);
        model.addAttribute("sortClass", classes);
    }
}

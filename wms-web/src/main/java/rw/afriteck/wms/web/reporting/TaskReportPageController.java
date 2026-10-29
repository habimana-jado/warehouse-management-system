package rw.afriteck.wms.web.reporting;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import rw.afriteck.wms.reporting.dtos.TaskBacklogResponse;
import rw.afriteck.wms.reporting.service.TaskReportService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.PageView;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Controller
@PreAuthorize(WebAccess.BOARD)
@RequiredArgsConstructor
class TaskReportPageController {

    private final TaskReportService service;
    private final WarehouseContext warehouseContext;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() { return List.of(15, 25, 50, 100); }

    @GetMapping("/reports/tasks/backlog")
    String backlog(@ModelAttribute("filter") ReportPageForm filter, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        TaskBacklogResponse report = service.backlog(warehouseId, filter.getPage(), filter.getSize());
        model.addAttribute("report", report);
        model.addAttribute("page", PageView.of(report.overdue(), Function.identity()));
        return Htmx.isFragmentRequest(request) ? "reporting/task-backlog :: results" : "reporting/task-backlog";
    }
}

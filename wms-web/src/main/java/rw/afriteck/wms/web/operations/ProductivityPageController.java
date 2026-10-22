package rw.afriteck.wms.web.operations;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.operations.dtos.ProductivityFilter;
import rw.afriteck.wms.operations.enums.TaskType;
import rw.afriteck.wms.operations.service.TaskProductivityService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.Htmx;
import rw.afriteck.wms.web.support.WarehouseContext;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/tasks/productivity")
@RequiredArgsConstructor
public class ProductivityPageController {

    private static final String VIEW = "operations/productivity";

    private final TaskProductivityService productivityService;
    private final WarehouseContext warehouseContext;
    private final Clock clock;

    @Value("${wms.ui.zone:Africa/Kigali}")
    private ZoneId zone;

    @ModelAttribute("types")
    TaskType[] types() { return TaskType.values(); }

    @GetMapping
    @PreAuthorize(WebAccess.MANAGER)
    public String productivity(@ModelAttribute("filter") ProductivityFilterForm filter,
                               Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (filter.getTo() == null) filter.setTo(today);
        if (filter.getFrom() == null) filter.setFrom(filter.getTo().minusDays(29));

        ProductivityFilter f = new ProductivityFilter(warehouseId,
                filter.getFrom().atStartOfDay(zone).toInstant(),
                filter.getTo().plusDays(1).atStartOfDay(zone).toInstant(),      // the service wants an exclusive end
                filter.getType());
        try {
            model.addAttribute("rows", productivityService.byWorker(f).stream().map(ProductivityRow::of).toList());
        } catch (BusinessException e) {                                          // e.g. INVALID_DATE_RANGE
            model.addAttribute("rows", List.<ProductivityRow>of());
            model.addAttribute("error", e.getMessage());
        }
        model.addAttribute("menu", "productivity");
        return Htmx.isFragmentRequest(request) ? VIEW + " :: results" : VIEW;
    }
}

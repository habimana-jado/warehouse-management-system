package rw.afriteck.wms.operations.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.operations.dtos.ProductivityFilter;
import rw.afriteck.wms.operations.dtos.WorkerProductivityResponse;
import rw.afriteck.wms.operations.service.TaskProductivityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wms/operations/tasks/productivity")
@RequiredArgsConstructor
public class TaskProductivityController {

    private final TaskProductivityService productivityService;
    private final UserContext userContext;

    @GetMapping
    @PreAuthorize(OperationsAccess.MANAGER)
    public List<WorkerProductivityResponse> byWorker(@Valid ProductivityFilter filter) {
        userContext.requireWarehouseAccess(filter.warehouseId());
        return productivityService.byWorker(filter);
    }
}

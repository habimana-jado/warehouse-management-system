package rw.afriteck.wms.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.enums.CountStatus;
import rw.afriteck.wms.inventory.service.CountService;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inventory/counts")
@RequiredArgsConstructor
public class CountController {

    private final CountService service;
    private final UserContext userContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(InventoryAccess.MANAGER)
    public CountResponse create(@RequestBody CreateCountRequest request) {
        userContext.requireWarehouseAccess(request.warehouseId());
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize(InventoryAccess.BOARD)
    public PageResponse<CountResponse> search(@RequestParam UUID warehouseId,
                                              @RequestParam(required = false) Set<CountStatus> status,
                                              @RequestParam(required = false) String q,
                                              Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return service.search(new CountFilter(warehouseId, status, q), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventoryAccess.ANY_USER)
    public CountResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.getById(id);
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize(InventoryAccess.BOARD)
    public PageResponse<CountLineResponse> lines(@PathVariable UUID id,
                                                 @RequestParam(defaultValue = "false") boolean onlyVariance,
                                                 Pageable pageable) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.lines(id, onlyVariance, pageable);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize(InventoryAccess.MANAGER)
    public CountResponse start(@PathVariable UUID id, @RequestBody(required = false) StartCountRequest request) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.start(id, request);
    }

    @PostMapping("/{id}/lines/{lineId}/count")
    @PreAuthorize(InventoryAccess.WORKER)
    public CountLineResponse recordCount(@PathVariable UUID id, @PathVariable UUID lineId,
                                         @RequestBody RecordCountRequest request) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.recordCount(id, lineId, request, userContext.currentUserId(), isManager());
    }

    @PostMapping("/{id}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(InventoryAccess.WORKER)
    public CountLineResponse addLine(@PathVariable UUID id, @RequestBody AddCountLineRequest request) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.addLine(id, request, userContext.currentUserId());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(InventoryAccess.MANAGER)
    public CountResponse submit(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.submit(id);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize(InventoryAccess.MANAGER)
    public CountResponse reopen(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.reopen(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(InventoryAccess.MANAGER)
    public CountResponse approve(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.approve(id, userContext.currentUserId());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(InventoryAccess.MANAGER)
    public CountResponse cancel(@PathVariable UUID id, @RequestBody CancelCountRequest request) {
        userContext.requireWarehouseAccess(service.warehouseIdOf(id));
        return service.cancel(id, request.reason());
    }

    private boolean isManager() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }
}

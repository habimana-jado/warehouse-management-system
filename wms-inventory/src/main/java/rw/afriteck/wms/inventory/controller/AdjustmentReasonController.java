package rw.afriteck.wms.inventory.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.inventory.dtos.AdjustmentReasonResponse;
import rw.afriteck.wms.inventory.dtos.CreateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.dtos.UpdateAdjustmentReasonRequest;
import rw.afriteck.wms.inventory.service.AdjustmentReasonService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/inventory/adjustment-reasons")
@RequiredArgsConstructor
public class AdjustmentReasonController {

    private final AdjustmentReasonService service;
    private final UserContext userContext;

    @GetMapping
    @PreAuthorize(InventoryAccess.ANY_USER)       // the create form needs the list; only admins see inactive ones
    public PageResponse<AdjustmentReasonResponse> search(@RequestParam(required = false) String q,
                                                         @RequestParam(defaultValue = "false") boolean includeInactive,
                                                         Pageable pageable) {
        return service.search(q, includeInactive && userContext.isAdmin(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventoryAccess.ANY_USER)
    public AdjustmentReasonResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(InventoryAccess.ADMIN)
    public AdjustmentReasonResponse create(@RequestBody CreateAdjustmentReasonRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(InventoryAccess.ADMIN)
    public AdjustmentReasonResponse update(@PathVariable UUID id, @RequestBody UpdateAdjustmentReasonRequest request) {
        return service.update(id, request);
    }
}

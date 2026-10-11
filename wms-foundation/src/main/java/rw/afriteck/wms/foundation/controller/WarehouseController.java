package rw.afriteck.wms.foundation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.AdminOnly;
import rw.afriteck.wms.common.security.AnyAuthenticatedUser;
import rw.afriteck.wms.common.security.ManagerOrAbove;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService service;
    private final LocationService locationService;
    private final UserContext userContext;

    @PostMapping
    @AdminOnly
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseResponse create(@Valid @RequestBody CreateWarehouseRequest r) {
        return service.create(r);
    }

    @GetMapping("/mine")                                    // for warehouse pickers in the UI
    @AnyAuthenticatedUser
    public List<WarehouseResponse> mine() {
        return service.findAccessibleToCurrentUser();
    }

    @GetMapping
    @AdminOnly
    public PageResponse<WarehouseResponse> search(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.of(service.search(q, pageable));
    }

    @GetMapping("/{id}")
    public WarehouseResponse get(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(id);
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @ManagerOrAbove
    public WarehouseResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWarehouseRequest r) {
        userContext.requireWarehouseAccess(id);
        return service.update(id, r);
    }

    @GetMapping("/{warehouseId}/locations")
    @AnyAuthenticatedUser
    public PageResponse<LocationResponse> searchLocationsByWarehouse(@PathVariable UUID warehouseId,
                                                                     @RequestParam(required = false) String q,
                                                                     @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        userContext.requireWarehouseAccess(warehouseId);
        return PageResponse.of(locationService.search(warehouseId, q, pageable));
    }

    @GetMapping("/{warehouseId}/locations/tree")
    @AnyAuthenticatedUser
    public List<LocationTreeResponse> listWarehouseLocationsTree(@PathVariable UUID warehouseId) {
        userContext.requireWarehouseAccess(warehouseId);
        return locationService.tree(warehouseId);
    }

    @GetMapping("/{warehouseId}/locations/by-barcode/{barcode}")
    @AnyAuthenticatedUser
    public LocationResponse byBarcode(@PathVariable UUID warehouseId, @PathVariable String barcode) {
        userContext.requireWarehouseAccess(warehouseId);
        return locationService.getByBarcode(warehouseId, barcode);
    }

    @PostMapping("/{id}/activate")
    @AdminOnly
    public WarehouseResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @ManagerOrAbove
    public WarehouseResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}

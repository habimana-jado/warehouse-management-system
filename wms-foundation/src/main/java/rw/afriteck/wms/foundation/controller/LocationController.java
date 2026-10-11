package rw.afriteck.wms.foundation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.security.AnyAuthenticatedUser;
import rw.afriteck.wms.common.security.ManagerOrAbove;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.CreateLocationRequest;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.UpdateLocationRequest;
import rw.afriteck.wms.foundation.service.LocationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService service;
    private final UserContext userContext;

    @PostMapping
    @ManagerOrAbove
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponse create(@Valid @RequestBody CreateLocationRequest r) {
        userContext.requireWarehouseAccess(r.warehouseId());
        return service.create(r);
    }

    @GetMapping("/{id}")
    @AnyAuthenticatedUser
    public LocationResponse get(@PathVariable UUID id) {
        LocationResponse l = service.getById(id);
        userContext.requireWarehouseAccess(l.warehouseId());
        return l;
    }

    @PutMapping("/{id}")
    @ManagerOrAbove
    public LocationResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateLocationRequest r) {
        userContext.requireWarehouseAccess(service.getById(id).warehouseId());
        return service.update(id, r);
    }

    @PostMapping("/{id}/activate")
    @ManagerOrAbove
    public LocationResponse activate(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.getById(id).warehouseId());
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @ManagerOrAbove
    public LocationResponse deactivate(@PathVariable UUID id) {
        userContext.requireWarehouseAccess(service.getById(id).warehouseId());
        return service.deactivate(id);
    }
}

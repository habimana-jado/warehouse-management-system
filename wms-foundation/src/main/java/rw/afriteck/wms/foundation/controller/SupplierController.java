package rw.afriteck.wms.foundation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.security.AnyAuthenticatedUser;
import rw.afriteck.wms.common.security.ManagerOrAbove;
import rw.afriteck.wms.foundation.dtos.CreateSupplierRequest;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.UpdateSupplierRequest;
import rw.afriteck.wms.foundation.service.SupplierService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @PostMapping
    @ManagerOrAbove
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest r) {
        return service.create(r);
    }

    @GetMapping("/{id}")
    @AnyAuthenticatedUser
    public SupplierResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @ManagerOrAbove
    public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateSupplierRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/activate")
    @ManagerOrAbove
    public SupplierResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @ManagerOrAbove
    public SupplierResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}
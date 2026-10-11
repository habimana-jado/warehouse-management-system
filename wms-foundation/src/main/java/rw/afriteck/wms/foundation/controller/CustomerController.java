package rw.afriteck.wms.foundation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.security.AnyAuthenticatedUser;
import rw.afriteck.wms.common.security.ManagerOrAbove;
import rw.afriteck.wms.foundation.dtos.CreateCustomerRequest;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.UpdateCustomerRequest;
import rw.afriteck.wms.foundation.service.CustomerService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    @ManagerOrAbove
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest r) {
        return service.create(r);
    }

    @GetMapping("/{id}")
    @AnyAuthenticatedUser
    public CustomerResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @ManagerOrAbove
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/activate")
    @ManagerOrAbove
    public CustomerResponse activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @ManagerOrAbove
    public CustomerResponse deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}
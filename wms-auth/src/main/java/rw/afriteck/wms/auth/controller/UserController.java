package rw.afriteck.wms.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.auth.dtos.*;
import rw.afriteck.wms.auth.service.UserService;
import rw.afriteck.wms.common.dtos.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService service;

    @GetMapping
    public PageResponse<UserResponse> search(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.of(service.search(q, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest r) { return service.create(r); }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest r) {
        return service.update(id, r);
    }

    @PutMapping("/{id}/roles")
    public UserResponse roles(@PathVariable UUID id, @Valid @RequestBody AssignRolesRequest r) {
        return service.assignRoles(id, r);
    }

    @PutMapping("/{id}/warehouses")
    public UserResponse warehouses(@PathVariable UUID id, @Valid @RequestBody AssignWarehousesRequest r) {
        return service.assignWarehouses(id, r);
    }

    @PostMapping("/{id}/activate")
    public UserResponse activate(@PathVariable UUID id) { return service.setActive(id, true); }

    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable UUID id) { return service.setActive(id, false); }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest r) {
        service.resetPassword(id, r);
    }
}

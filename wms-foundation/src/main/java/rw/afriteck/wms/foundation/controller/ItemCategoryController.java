package rw.afriteck.wms.foundation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.security.AnyAuthenticatedUser;
import rw.afriteck.wms.common.security.ManagerOrAbove;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.service.ItemCategoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/item-categories")
@RequiredArgsConstructor
public class ItemCategoryController {

    private final ItemCategoryService service;

    @PostMapping
    @ManagerOrAbove
    @ResponseStatus(HttpStatus.CREATED)
    public ItemCategoryResponse create(@Valid @RequestBody CreateItemCategoryRequest r) {
        return service.create(r);
    }

    @GetMapping("/{id}")
    @AnyAuthenticatedUser
    public ItemCategoryResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @GetMapping
    @AnyAuthenticatedUser
    public List<ItemCategoryResponse> getAllActive() {
        return service.findAllActive();
    }

    @PutMapping("/{id}")
    @ManagerOrAbove
    public ItemCategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateItemCategoryRequest r) {
        return service.update(id, r);
    }

    @PostMapping("/{id}/activate")
    @ManagerOrAbove
    public ItemCategoryResponse activate(@PathVariable UUID id) {
        return service.setActive(id, true);
    }

    @PostMapping("/{id}/deactivate")
    @ManagerOrAbove
    public ItemCategoryResponse deactivate(@PathVariable UUID id) {
        return service.setActive(id, false);
    }
}
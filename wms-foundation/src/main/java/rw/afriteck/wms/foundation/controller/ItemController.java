package rw.afriteck.wms.foundation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.security.AnyAuthenticatedUser;
import rw.afriteck.wms.common.security.ManagerOrAbove;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.service.ItemService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wms/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService service;

    @PostMapping
    @ManagerOrAbove
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponseDetailed create(@Valid @RequestBody CreateItemRequest r) {
        return service.create(r);
    }

    @GetMapping
    @AnyAuthenticatedUser
    public PageResponse<ItemResponse> search(ItemFilter filter,
                                             @PageableDefault(size = 20, sort = "sku") Pageable pageable) {
        return PageResponse.of(service.search(filter, pageable));
    }

    @GetMapping("/{id}")
    @AnyAuthenticatedUser
    public ItemResponseDetailed get(@PathVariable UUID id) {
        return service.getDetailedById(id);
    }

    //TODO: To Implement & Test with Scanning Device
    @GetMapping("/scan/{barcode}")                         // scanner endpoint: item or box barcode
    @AnyAuthenticatedUser
    public BarcodeResolution scan(@PathVariable String barcode) {
        return service.resolveBarcode(barcode);
    }

    @PutMapping("/{id}")
    @ManagerOrAbove
    public ItemResponseDetailed update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest r) {
        return service.update(id, r);
    }

    @PutMapping("/{id}/uoms")
    @ManagerOrAbove
    public ItemResponseDetailed replaceUoms(@PathVariable UUID id, @Valid @RequestBody ReplaceItemUomsRequest r) {
        return service.replaceUoms(id, r);
    }

    @PostMapping("/{id}/activate")
    @ManagerOrAbove
    public ItemResponseDetailed activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    @ManagerOrAbove
    public ItemResponseDetailed deactivate(@PathVariable UUID id) {
        return service.deactivate(id);
    }
}
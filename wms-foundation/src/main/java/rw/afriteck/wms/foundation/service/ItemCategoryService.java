package rw.afriteck.wms.foundation.service;

import rw.afriteck.wms.foundation.dtos.CreateItemCategoryRequest;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.dtos.UpdateItemCategoryRequest;

import java.util.List;
import java.util.UUID;

public interface ItemCategoryService {

    ItemCategoryResponse create(CreateItemCategoryRequest r);

    ItemCategoryResponse update(UUID id, UpdateItemCategoryRequest r);

    ItemCategoryResponse setActive(UUID id, boolean active);

    ItemCategoryResponse getById(UUID id);

    List<ItemCategoryResponse> findAllActive();
}

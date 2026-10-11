package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.CreateItemCategoryRequest;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.dtos.UpdateItemCategoryRequest;
import rw.afriteck.wms.foundation.entity.ItemCategory;
import rw.afriteck.wms.foundation.repository.ItemCategoryRepository;
import rw.afriteck.wms.foundation.service.ItemCategoryService;
import rw.afriteck.wms.foundation.support.Codes;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemCategoryServiceImpl implements ItemCategoryService {

    private final ItemCategoryRepository categories;

    @Override
    public ItemCategoryResponse create(CreateItemCategoryRequest r) {
        String name = r.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ConflictException("CATEGORY_EXISTS", "Category already exists: " + name);
        }
        ItemCategory c = new ItemCategory();
        c.setName(name);
        c.setDescription(Codes.blankToNull(r.description()));
        return toResponse(categories.save(c));
    }

    @Override
    public ItemCategoryResponse update(UUID id, UpdateItemCategoryRequest r) {
        ItemCategory c = find(id);
        String name = r.name().trim();
        if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("CATEGORY_EXISTS", "Category already exists: " + name);
        }
        c.setName(name);
        c.setDescription(Codes.blankToNull(r.description()));
        return toResponse(c);
    }

    @Override
    public ItemCategoryResponse setActive(UUID id, boolean active) {
        ItemCategory c = find(id);
        c.setActive(active);
        return toResponse(c);
    }

    @Override @Transactional(readOnly = true)
    public ItemCategoryResponse getById(UUID id) { return toResponse(find(id)); }

    @Override @Transactional(readOnly = true)
    public List<ItemCategoryResponse> findAllActive() {
        return categories.findByActiveTrueOrderByName().stream().map(this::toResponse).toList();
    }

    private ItemCategory find(UUID id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Item category", id));
    }

    private ItemCategoryResponse toResponse(ItemCategory c) {
        return new ItemCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }
}

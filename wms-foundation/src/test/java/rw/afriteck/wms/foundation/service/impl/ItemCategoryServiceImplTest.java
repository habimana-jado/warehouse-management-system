package rw.afriteck.wms.foundation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.CreateItemCategoryRequest;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.dtos.UpdateItemCategoryRequest;
import rw.afriteck.wms.foundation.entity.ItemCategory;
import rw.afriteck.wms.foundation.repository.ItemCategoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemCategoryServiceImplTest {

    @Mock
    private ItemCategoryRepository categories;

    private ItemCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemCategoryServiceImpl(categories);
    }

    @Test
    void create_Success() {
        CreateItemCategoryRequest request = new CreateItemCategoryRequest(" Electronics ", " Gadgets ");

        when(categories.existsByNameIgnoreCase("Electronics")).thenReturn(false);

        ItemCategory saved = new ItemCategory();
        saved.setId(UUID.randomUUID());
        saved.setName("Electronics");
        saved.setDescription("Gadgets");
        saved.setActive(true);

        when(categories.save(any(ItemCategory.class))).thenReturn(saved);

        ItemCategoryResponse res = service.create(request);

        assertNotNull(res);
        assertEquals("Electronics", res.name());
        assertEquals("Gadgets", res.description());
        assertTrue(res.active());
    }

    @Test
    void create_DuplicateName_ThrowsConflictException() {
        CreateItemCategoryRequest request = new CreateItemCategoryRequest("Electronics", "Desc");

        when(categories.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertEquals("CATEGORY_EXISTS", ex.getCode());
    }

    @Test
    void update_Success() {
        UUID id = UUID.randomUUID();
        ItemCategory existing = new ItemCategory();
        existing.setId(id);
        existing.setName("Old Name");
        existing.setActive(true);

        UpdateItemCategoryRequest request = new UpdateItemCategoryRequest("New Name", "New Desc");

        when(categories.findById(id)).thenReturn(Optional.of(existing));

        ItemCategoryResponse res = service.update(id, request);

        assertEquals("New Name", res.name());
        assertEquals("New Desc", res.description());
    }

    @Test
    void findAllActive_Success() {
        ItemCategory c1 = new ItemCategory();
        c1.setId(UUID.randomUUID());
        c1.setName("Category 1");
        c1.setActive(true);

        when(categories.findByActiveTrueOrderByName()).thenReturn(List.of(c1));

        List<ItemCategoryResponse> result = service.findAllActive();

        assertEquals(1, result.size());
        assertEquals("Category 1", result.get(0).name());
    }

    @Test
    void getById_NotFound_ThrowsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(categories.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getById(id));
    }
}

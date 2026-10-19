package rw.afriteck.wms.foundation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.afriteck.wms.foundation.dtos.CreateItemCategoryRequest;
import rw.afriteck.wms.foundation.dtos.ItemCategoryResponse;
import rw.afriteck.wms.foundation.dtos.UpdateItemCategoryRequest;
import rw.afriteck.wms.foundation.service.ItemCategoryService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemCategoryControllerTest {

    @Mock
    private ItemCategoryService service;

    private ItemCategoryController controller;

    @BeforeEach
    void setUp() {
        controller = new ItemCategoryController(service);
    }

    @Test
    void create_DelegatesToService() {
        CreateItemCategoryRequest request = new CreateItemCategoryRequest("Cat1", "Desc");
        ItemCategoryResponse expected = new ItemCategoryResponse(UUID.randomUUID(), "Cat1", "Desc", true);

        when(service.create(request)).thenReturn(expected);

        ItemCategoryResponse actual = controller.create(request);

        assertEquals(expected, actual);
        verify(service).create(request);
    }

    @Test
    void get_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ItemCategoryResponse expected = new ItemCategoryResponse(id, "Cat1", "Desc", true);

        when(service.getById(id)).thenReturn(expected);

        ItemCategoryResponse actual = controller.get(id);

        assertEquals(expected, actual);
        verify(service).getById(id);
    }

    @Test
    void getAllActive_DelegatesToService() {
        ItemCategoryResponse expected = new ItemCategoryResponse(UUID.randomUUID(), "Cat1", "Desc", true);
        when(service.findAllActive()).thenReturn(List.of(expected));

        List<ItemCategoryResponse> actual = controller.getAllActive();

        assertEquals(1, actual.size());
        assertEquals(expected, actual.get(0));
        verify(service).findAllActive();
    }

    @Test
    void update_DelegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateItemCategoryRequest request = new UpdateItemCategoryRequest("Cat2", "Desc2");
        ItemCategoryResponse expected = new ItemCategoryResponse(id, "Cat2", "Desc2", true);

        when(service.update(id, request)).thenReturn(expected);

        ItemCategoryResponse actual = controller.update(id, request);

        assertEquals(expected, actual);
        verify(service).update(id, request);
    }

    @Test
    void activate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ItemCategoryResponse expected = new ItemCategoryResponse(id, "Cat1", "Desc", true);

        when(service.setActive(id, true)).thenReturn(expected);

        ItemCategoryResponse actual = controller.activate(id);

        assertEquals(expected, actual);
        verify(service).setActive(id, true);
    }

    @Test
    void deactivate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ItemCategoryResponse expected = new ItemCategoryResponse(id, "Cat1", "Desc", false);

        when(service.setActive(id, false)).thenReturn(expected);

        ItemCategoryResponse actual = controller.deactivate(id);

        assertEquals(expected, actual);
        verify(service).setActive(id, false);
    }
}

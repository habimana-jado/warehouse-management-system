package rw.afriteck.wms.foundation.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.service.ItemService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemService service;

    private ItemController controller;

    @BeforeEach
    void setUp() {
        controller = new ItemController(service);
    }

    @Test
    void create_DelegatesToService() {
        CreateItemRequest request = new CreateItemRequest("SKU1", "Item 1", null, null, null, "PCS", TrackingType.NONE, false, null, null, List.of());
        ItemResponseDetailed expected = new ItemResponseDetailed(UUID.randomUUID(), "SKU1", "Item 1", null, null, null, null, "PCS", TrackingType.NONE, false, null, null, true, List.of(), null, null);

        when(service.create(request)).thenReturn(expected);

        ItemResponseDetailed actual = controller.create(request);

        assertEquals(expected, actual);
        verify(service).create(request);
    }

    @Test
    void search_DelegatesToService() {
        ItemFilter filter = new ItemFilter(null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        ItemResponse itemResp = new ItemResponse(UUID.randomUUID(), "SKU1", "Item 1", null, null, null, "PCS", TrackingType.NONE, false, true);
        Page<ItemResponse> page = new PageImpl<>(List.of(itemResp));

        when(service.search(filter, pageable)).thenReturn(page);

        var res = controller.search(filter, pageable);

        assertNotNull(res);
        assertEquals(1, res.content().size());
        verify(service).search(filter, pageable);
    }

    @Test
    void get_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ItemResponseDetailed expected = new ItemResponseDetailed(id, "SKU1", "Item 1", null, null, null, null, "PCS", TrackingType.NONE, false, null, null, true, List.of(), null, null);

        when(service.getDetailedById(id)).thenReturn(expected);

        ItemResponseDetailed actual = controller.get(id);

        assertEquals(expected, actual);
        verify(service).getDetailedById(id);
    }

    @Test
    void scan_DelegatesToService() {
        ItemResponse itemResp = new ItemResponse(UUID.randomUUID(), "SKU1", "Item 1", null, null, null, "PCS", TrackingType.NONE, false, true);
        BarcodeResolution expected = new BarcodeResolution(itemResp, "PCS", BigDecimal.ONE);

        when(service.resolveBarcode("BARCODE123")).thenReturn(expected);

        BarcodeResolution actual = controller.scan("BARCODE123");

        assertEquals(expected, actual);
        verify(service).resolveBarcode("BARCODE123");
    }

    @Test
    void update_DelegatesToService() {
        UUID id = UUID.randomUUID();
        UpdateItemRequest request = new UpdateItemRequest("New Name", null, null, null, null, null);
        ItemResponseDetailed expected = new ItemResponseDetailed(id, "SKU1", "New Name", null, null, null, null, "PCS", TrackingType.NONE, false, null, null, true, List.of(), null, null);

        when(service.update(id, request)).thenReturn(expected);

        ItemResponseDetailed actual = controller.update(id, request);

        assertEquals(expected, actual);
        verify(service).update(id, request);
    }

    @Test
    void replaceUoms_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ReplaceItemUomsRequest request = new ReplaceItemUomsRequest(List.of());
        ItemResponseDetailed expected = new ItemResponseDetailed(id, "SKU1", "Item 1", null, null, null, null, "PCS", TrackingType.NONE, false, null, null, true, List.of(), null, null);

        when(service.replaceUoms(id, request)).thenReturn(expected);

        ItemResponseDetailed actual = controller.replaceUoms(id, request);

        assertEquals(expected, actual);
        verify(service).replaceUoms(id, request);
    }

    @Test
    void activate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ItemResponseDetailed expected = new ItemResponseDetailed(id, "SKU1", "Item 1", null, null, null, null, "PCS", TrackingType.NONE, false, null, null, true, List.of(), null, null);

        when(service.activate(id)).thenReturn(expected);

        ItemResponseDetailed actual = controller.activate(id);

        assertEquals(expected, actual);
        verify(service).activate(id);
    }

    @Test
    void deactivate_DelegatesToService() {
        UUID id = UUID.randomUUID();
        ItemResponseDetailed expected = new ItemResponseDetailed(id, "SKU1", "Item 1", null, null, null, null, "PCS", TrackingType.NONE, false, null, null, false, List.of(), null, null);

        when(service.deactivate(id)).thenReturn(expected);

        ItemResponseDetailed actual = controller.deactivate(id);

        assertEquals(expected, actual);
        verify(service).deactivate(id);
    }
}

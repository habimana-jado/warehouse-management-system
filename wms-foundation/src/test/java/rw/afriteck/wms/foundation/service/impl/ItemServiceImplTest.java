package rw.afriteck.wms.foundation.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.entity.Item;
import rw.afriteck.wms.foundation.entity.ItemCategory;
import rw.afriteck.wms.foundation.entity.ItemUom;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.repository.ItemCategoryRepository;
import rw.afriteck.wms.foundation.repository.ItemRepository;
import rw.afriteck.wms.foundation.repository.ItemUomRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository items;
    @Mock
    private ItemUomRepository uomRepo;
    @Mock
    private ItemCategoryRepository categories;
    @Mock
    private DeactivationGuards guards;

    private ItemServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ItemServiceImpl(items, uomRepo, categories, guards);
    }

    @Test
    void create_Success() {
        UUID categoryId = UUID.randomUUID();
        ItemCategory category = new ItemCategory();
        category.setId(categoryId);
        category.setName("Electronics");

        CreateItemRequest request = new CreateItemRequest(
                " SKU-01 ", "Laptop", "High end laptop", "EAN-123", categoryId,
                "PCS", TrackingType.SERIAL, false, new BigDecimal("1000.00"), new BigDecimal("2.5"),
                List.of(new ItemUomRequest("BOX", new BigDecimal("10.0"), "BOX-123"))
        );

        when(items.existsBySkuIgnoreCase("SKU-01")).thenReturn(false);
        when(categories.findById(categoryId)).thenReturn(Optional.of(category));

        Item savedItem = new Item();
        savedItem.setId(UUID.randomUUID());
        savedItem.setSku("SKU-01");
        savedItem.setName("Laptop");
        savedItem.setCategory(category);
        savedItem.setBaseUom("PCS");
        savedItem.setTrackingType(TrackingType.SERIAL);
        savedItem.setUoms(new ArrayList<>());
        savedItem.setActive(true);

        when(items.save(any(Item.class))).thenReturn(savedItem);

        ItemResponseDetailed res = service.create(request);

        assertNotNull(res);
        assertEquals("SKU-01", res.sku());
        assertEquals("Laptop", res.name());
    }

    @Test
    void create_DuplicateSku_ThrowsConflictException() {
        CreateItemRequest request = new CreateItemRequest(
                "SKU-01", "Laptop", null, null, null,
                "PCS", TrackingType.NONE, false, null, null, List.of()
        );

        when(items.existsBySkuIgnoreCase("SKU-01")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(request));
        assertEquals("SKU_EXISTS", ex.getCode());
    }

    @Test
    void create_DuplicateUomCode_ThrowsBusinessException() {
        UUID categoryId = UUID.randomUUID();
        ItemCategory category = new ItemCategory();
        category.setId(categoryId);

        CreateItemRequest request = new CreateItemRequest(
                "SKU-01", "Laptop", null, null, categoryId,
                "PCS", TrackingType.NONE, false, null, null,
                List.of(
                        new ItemUomRequest("BOX", new BigDecimal("10.0"), null),
                        new ItemUomRequest("BOX", new BigDecimal("12.0"), null)
                )
        );

        when(items.existsBySkuIgnoreCase("SKU-01")).thenReturn(false);
        when(categories.findById(categoryId)).thenReturn(Optional.of(category));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertEquals("DUPLICATE_UOM", ex.getCode());
    }

    @Test
    void resolveBarcode_FoundOnItemBarcode() {
        Item item = new Item();
        item.setId(UUID.randomUUID());
        item.setSku("SKU-01");
        item.setBarcode("123456789");
        item.setBaseUom("PCS");

        when(items.findByBarcode("123456789")).thenReturn(Optional.of(item));

        BarcodeResolution res = service.resolveBarcode("123456789");

        assertNotNull(res);
        assertEquals(item.getId(), res.item().id());
        assertEquals("SKU-01", res.item().sku());
        assertEquals("PCS", res.uom());
        assertEquals(BigDecimal.ONE, res.factorToBase());
    }

    @Test
    void resolveBarcode_FoundOnUomBarcode() {
        when(items.findByBarcode("BOX-BARCODE")).thenReturn(Optional.empty());

        Item item = new Item();
        item.setId(UUID.randomUUID());
        item.setSku("SKU-01");

        ItemUom uom = new ItemUom();
        uom.setItem(item);
        uom.setUom("BOX");
        uom.setFactorToBase(new BigDecimal("12.0"));
        uom.setBarcode("BOX-BARCODE");

        when(uomRepo.findByBarcode("BOX-BARCODE")).thenReturn(Optional.of(uom));

        BarcodeResolution res = service.resolveBarcode("BOX-BARCODE");

        assertNotNull(res);
        assertEquals(item.getId(), res.item().id());
        assertEquals("BOX", res.uom());
        assertEquals(new BigDecimal("12.0"), res.factorToBase());
    }

    @Test
    void replaceUoms_Success() {
        UUID itemId = UUID.randomUUID();
        Item item = new Item();
        item.setId(itemId);
        item.setBaseUom("PCS");
        item.setUoms(new ArrayList<>());

        ReplaceItemUomsRequest request = new ReplaceItemUomsRequest(
                List.of(new ItemUomRequest("BOX", new BigDecimal("12.0"), "BOX-BAR"))
        );

        when(items.findWithUomsById(itemId)).thenReturn(Optional.of(item));

        ItemResponseDetailed res = service.replaceUoms(itemId, request);

        assertNotNull(res);
        assertEquals(1, item.getUoms().size());
        assertEquals("BOX", item.getUoms().get(0).getUom());
    }

    @Test
    void deactivate_WithGuardCheck() {
        UUID id = UUID.randomUUID();
        Item item = new Item();
        item.setId(id);
        item.setActive(true);
        item.setUoms(new ArrayList<>());

        when(items.findWithUomsById(id)).thenReturn(Optional.of(item));

        ItemResponseDetailed res = service.deactivate(id);

        assertFalse(res.active());
        verify(guards).check(DeactivationGuard.Target.ITEM, id);
    }
}

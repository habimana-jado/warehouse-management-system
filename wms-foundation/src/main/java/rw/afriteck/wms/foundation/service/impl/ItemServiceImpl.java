package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.*;
import rw.afriteck.wms.foundation.entity.Item;
import rw.afriteck.wms.foundation.entity.ItemCategory;
import rw.afriteck.wms.foundation.entity.ItemUom;
import rw.afriteck.wms.foundation.enums.TrackingType;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.mapper.ItemMapper;
import rw.afriteck.wms.foundation.repository.ItemCategoryRepository;
import rw.afriteck.wms.foundation.repository.ItemRepository;
import rw.afriteck.wms.foundation.repository.ItemUomRepository;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.specifications.ItemSpecs;
import rw.afriteck.wms.common.support.Codes;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository items;
    private final ItemUomRepository uomRepo;
    private final ItemCategoryRepository categories;
    private final DeactivationGuards guards;

    @Override
    public ItemResponseDetailed create(CreateItemRequest r) {
        String sku = Codes.normalize(r.sku());
        if (items.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("SKU_EXISTS", "SKU already exists: " + sku);
        }
        if (r.expiryTracked() && r.trackingType() != TrackingType.LOT) {
            throw new BusinessException("INVALID_TRACKING", "Expiry tracking requires LOT tracking");
        }
        String barcode = Codes.blankToNull(r.barcode());
        assertBarcodeFree(barcode, null);

        Item item = new Item();
        item.setSku(sku);
        item.setName(r.name().trim());
        item.setDescription(Codes.blankToNull(r.description()));
        item.setBarcode(barcode);
        item.setBaseUom(Codes.normalize(r.baseUom()));
        item.setTrackingType(r.trackingType());
        item.setExpiryTracked(r.expiryTracked());
        item.setUnitCost(r.unitCost());
        item.setWeightKg(r.weightKg());
        item.setCategory(resolveCategory(r.categoryId()));
        syncUoms(item, r.uoms() == null ? List.of() : r.uoms());

        return ItemMapper.toDetailed(items.save(item));
    }

    @Override
    public ItemResponseDetailed update(UUID id, UpdateItemRequest r) {
        Item item = findWithUoms(id);

        String barcode = Codes.blankToNull(r.barcode());
        if (barcode != null && !barcode.equals(item.getBarcode())) {
            assertBarcodeFree(barcode, item.getId());
        }
        item.setName(r.name().trim());
        item.setDescription(Codes.blankToNull(r.description()));
        item.setBarcode(barcode);
        item.setUnitCost(r.unitCost());
        item.setWeightKg(r.weightKg());
        item.setCategory(resolveCategory(r.categoryId()));
        return ItemMapper.toDetailed(item);
    }

    @Override
    public ItemResponseDetailed replaceUoms(UUID id, ReplaceItemUomsRequest r) {
        Item item = findWithUoms(id);
        syncUoms(item, r.uoms());
        return ItemMapper.toDetailed(item);
    }

    @Override
    public ItemResponseDetailed activate(UUID id) {
        Item item = findWithUoms(id);
        item.setActive(true);
        return ItemMapper.toDetailed(item);
    }

    @Override
    public ItemResponseDetailed deactivate(UUID id) {
        Item item = findWithUoms(id);
        guards.check(DeactivationGuard.Target.ITEM, id);           // e.g. inventory: stock on hand
        item.setActive(false);
        return ItemMapper.toDetailed(item);
    }

    @Override @Transactional(readOnly = true)
    public ItemResponseDetailed getDetailedById(UUID id) {
        return ItemMapper.toDetailed(findWithUoms(id));
    }

    @Override @Transactional(readOnly = true)
    public Page<ItemResponse> search(ItemFilter filter, Pageable pageable) {
        return items.findAll(ItemSpecs.from(filter), pageable).map(ItemMapper::toResponse);
    }

    @Override @Transactional(readOnly = true)
    public ItemResponse getById(UUID id) {
        return ItemMapper.toResponse(items.findWithUomsById(id)
                .orElseThrow(() -> new NotFoundException("Item", id)));
    }

    @Override @Transactional(readOnly = true)
    public List<ItemResponse> findAllByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return items.findByIdIn(ids).stream().map(ItemMapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public ItemResponse getByBarcode(String barcode) {
        return items.findByBarcode(barcode.trim()).map(ItemMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Item barcode", barcode));
    }

    @Override @Transactional(readOnly = true)
    public BarcodeResolution resolveBarcode(String barcode) {
        String code = barcode.trim();

        Optional<Item> direct = items.findByBarcode(code);
        if (direct.isPresent()) {
            Item i = direct.get();
            return new BarcodeResolution(ItemMapper.toResponse(i), i.getBaseUom(), BigDecimal.ONE);
        }
        ItemUom u = uomRepo.findByBarcode(code)
                .orElseThrow(() -> new NotFoundException("Barcode", code));
        return new BarcodeResolution(ItemMapper.toResponse(u.getItem()), u.getUom(), u.getFactorToBase());
    }

    @Override @Transactional(readOnly = true)
    public BigDecimal toBaseQuantity(UUID itemId, String uom, BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            throw new BusinessException("INVALID_QUANTITY", "Quantity must be greater than zero");
        }
        Item item = findWithUoms(itemId);
        String code = Codes.normalize(uom);
        if (code.equals(item.getBaseUom())) return qty;

        return item.getUoms().stream()
                .filter(u -> u.getUom().equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("UNKNOWN_UOM",
                        "Item " + item.getSku() + " has no unit of measure " + code))
                .getFactorToBase().multiply(qty);
    }

    // ---------- helpers ----------

    private Item findWithUoms(UUID id) {
        return items.findWithUomsById(id).orElseThrow(() -> new NotFoundException("Item", id));
    }

    private ItemCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categories.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Item category", categoryId));
    }

    private void assertBarcodeFree(String barcode, UUID itemId) {
        if (barcode == null) return;
        boolean taken = itemId == null
                ? items.existsByBarcode(barcode) || uomRepo.existsByBarcode(barcode)
                : items.existsByBarcodeAndIdNot(barcode, itemId) || uomRepo.existsByBarcodeAndItemIdNot(barcode, itemId);
        if (taken) throw new ConflictException("BARCODE_EXISTS", "Barcode already in use: " + barcode);
    }

    /**
     * Diff-based sync: update matching UOMs in place, remove missing, add new.
     * (Clearing and re-adding would violate the unique (item_id, uom) constraint,
     * because Hibernate runs inserts before deletes.)
     */
    private void syncUoms(Item item, List<ItemUomRequest> requested) {
        Map<String, ItemUomRequest> wanted = new LinkedHashMap<>();
        Set<String> barcodes = new HashSet<>();
        if (item.getBarcode() != null) barcodes.add(item.getBarcode());

        for (ItemUomRequest u : requested) {
            String code = Codes.normalize(u.uom());
            if (code.equals(item.getBaseUom())) {
                throw new BusinessException("INVALID_UOM", "The base unit " + code + " needs no conversion entry");
            }
            if (wanted.put(code, u) != null) {
                throw new BusinessException("DUPLICATE_UOM", "Unit of measure listed twice: " + code);
            }
            String bc = Codes.blankToNull(u.barcode());
            if (bc != null) {
                if (!barcodes.add(bc)) {
                    throw new BusinessException("DUPLICATE_BARCODE", "Barcode used twice in this item: " + bc);
                }
                assertBarcodeFree(bc, item.getId());
            }
        }

        item.getUoms().removeIf(existing -> !wanted.containsKey(existing.getUom()));

        wanted.forEach((code, u) -> {
            ItemUom existing = item.getUoms().stream()
                    .filter(e -> e.getUom().equals(code)).findFirst().orElse(null);
            if (existing == null) {
                existing = new ItemUom();
                existing.setItem(item);
                existing.setUom(code);
                item.getUoms().add(existing);
            }
            existing.setFactorToBase(u.factorToBase());
            existing.setBarcode(Codes.blankToNull(u.barcode()));
        });
    }
}
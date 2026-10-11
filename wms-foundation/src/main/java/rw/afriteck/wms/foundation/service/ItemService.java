package rw.afriteck.wms.foundation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rw.afriteck.wms.foundation.dtos.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ItemService {
    ItemResponseDetailed create(CreateItemRequest r);

    ItemResponseDetailed update(UUID id, UpdateItemRequest r);

    ItemResponseDetailed replaceUoms(UUID id, ReplaceItemUomsRequest r);

    ItemResponseDetailed activate(UUID id);

    ItemResponseDetailed deactivate(UUID id);

    ItemResponseDetailed getDetailedById(UUID id);

    Page<ItemResponse> search(ItemFilter filter, Pageable pageable);

    // Lookups for other modules
    ItemResponse getById(UUID id);

    List<ItemResponse> findAllByIds(Collection<UUID> ids);       // batch, avoids N+1

    ItemResponse getByBarcode(String barcode);                    // item barcode only

    BarcodeResolution resolveBarcode(String barcode);             // item or UOM barcode (scanner use)

    BigDecimal toBaseQuantity(UUID itemId, String uom, BigDecimal qty);
}

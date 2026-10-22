package rw.afriteck.wms.web.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.service.ItemService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemResolver {

    private static final int MAX_CANDIDATES = 10;

    private final ItemService itemService;

    public record Resolution(ItemResponse item, List<ItemResponse> candidates) {
        public boolean found()     { return item != null; }
        public boolean ambiguous() { return item == null && !candidates.isEmpty(); }
        public boolean unknown()   { return item == null && candidates.isEmpty(); }

        /** SKUs of the matching items, for the "several items match" message. */
        public String candidateSkus() {
            return candidates.stream().map(ItemResponse::sku).collect(Collectors.joining(", "));
        }
    }

    public Resolution resolve(String input) {
        String text = input == null ? "" : input.trim();
        if (text.isEmpty()) {
            return new Resolution(null, List.of());
        }
        try {                                                         // 1. exact barcode (scanner)
            return new Resolution(itemService.getByBarcode(text), List.of());
        } catch (NotFoundException ignored) {
            // not a barcode: fall through to the text search
        }
        List<ItemResponse> found = itemService.searchForPicker(text, MAX_CANDIDATES);
        List<ItemResponse> exactSku = found.stream().filter(i -> i.sku().equalsIgnoreCase(text)).toList();
        if (exactSku.size() == 1) {                                   // 2. exact SKU
            return new Resolution(exactSku.get(0), List.of());
        }
        if (found.size() == 1) {                                      // 3. only one match
            return new Resolution(found.get(0), List.of());
        }
        return new Resolution(null, found);                           // 4. several, or 5. none
    }
}

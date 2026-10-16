package rw.afriteck.wms.outbound.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/** Suggests the bin and lot for each pick line. Suggestions only: the picker can scan something else. FEFO is NOT enforced. */
@Component
@RequiredArgsConstructor
public class PickSuggester {

    private final InventoryQueryService inventoryQuery;
    private final Clock clock;

    public record Need(UUID itemId, BigDecimal qty) {}
    public record Suggestion(UUID locationId, String lotNumber) {}      // both null when no usable stock

    /** Result is aligned with the needs list. Stock already suggested for an earlier need in the same call is not suggested twice. */
    public List<Suggestion> suggest(UUID warehouseId, List<Need> needs) {
        LocalDate today = LocalDate.now(clock);
        Map<UUID, List<StockLevelResponse>> rowsByItem = new HashMap<>();
        Map<UUID, BigDecimal> taken = new HashMap<>();                  // stock row id -> quantity already suggested
        List<Suggestion> result = new ArrayList<>(needs.size());

        for (Need need : needs) {
            List<StockLevelResponse> rows = rowsByItem.computeIfAbsent(need.itemId(), id ->
                    inventoryQuery.findAvailable(warehouseId, id).stream()
                            .filter(r -> r.locationUsage() == LocationUsage.STORAGE)
                            .filter(r -> r.expiryDate() == null || !r.expiryDate().isBefore(today))   // never suggest an expired lot
                            .toList());

            Comparator<StockLevelResponse> order = Comparator
                    .comparing(StockLevelResponse::expiryDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                    .thenComparing(r -> left(r, taken).compareTo(need.qty()) >= 0 ? 0 : 1)       // a row that covers the need first
                    .thenComparing(r -> covers(r, taken, need) ? left(r, taken) : left(r, taken).negate())  // smallest covering, else largest
                    .thenComparing(StockLevelResponse::locationCode);

            Optional<StockLevelResponse> best = rows.stream()
                    .filter(r -> left(r, taken).signum() > 0)
                    .min(order);

            if (best.isEmpty()) {
                result.add(new Suggestion(null, null));
                continue;
            }
            StockLevelResponse r = best.get();
            taken.merge(r.id(), need.qty().min(left(r, taken)), BigDecimal::add);
            result.add(new Suggestion(r.locationId(), r.lotNumber()));
        }
        return result;
    }

    private static BigDecimal left(StockLevelResponse r, Map<UUID, BigDecimal> taken) {
        return r.quantity().subtract(taken.getOrDefault(r.id(), BigDecimal.ZERO));
    }
    private static boolean covers(StockLevelResponse r, Map<UUID, BigDecimal> taken, Need need) {
        return left(r, taken).compareTo(need.qty()) >= 0;
    }
}

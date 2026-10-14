package rw.afriteck.wms.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.support.Codes;
import rw.afriteck.wms.inventory.dtos.MovementResponse;
import rw.afriteck.wms.inventory.dtos.PostStockLine;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.dtos.PostStockResponse;
import rw.afriteck.wms.inventory.entity.Lot;
import rw.afriteck.wms.inventory.entity.StockBalance;
import rw.afriteck.wms.inventory.entity.StockMovement;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.mapper.MovementMapper;
import rw.afriteck.wms.inventory.repository.LotRepository;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockMovementRepository;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.inventory.support.InventoryCodes;
import rw.afriteck.wms.inventory.support.StockLineRules;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private static final int QTY_SCALE = 4;

    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final LotRepository lots;
    private final ItemService itemService;
    private final LocationService locationService;
    private final Clock clock;

    // ---- internal value types ----
    private record PreparedLine(MovementType type, LocationResponse location, ItemResponse item,
                                String lotNumber, LocalDate expiryDate, String serial,
                                BigDecimal delta, BigDecimal unitCost) {}

    private record LotKey(UUID itemId, String lotNumber) {}

    private record BalanceKey(UUID locationId, UUID itemId, UUID lotId, String serial) {}

    private record CheckKey(UUID locationId, UUID itemId, String lotNumber, String serial) {}

    private static final Comparator<BalanceKey> KEY_ORDER = Comparator
            .comparing(BalanceKey::locationId)
            .thenComparing(BalanceKey::itemId)
            .thenComparing(BalanceKey::lotId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(BalanceKey::serial, Comparator.nullsFirst(Comparator.naturalOrder()));

    @Override
    @Transactional
    public PostStockResponse post(PostStockRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessException("STOCK_NO_LINES", "At least one stock line is required");
        }
        UUID transactionId = request.transactionId() != null ? request.transactionId() : UUID.randomUUID();
        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : clock.instant();
        String referenceType = request.referenceType() == null ? null : Codes.blankToNull(Codes.normalize(request.referenceType()));
        String referenceNumber = Codes.blankToNull(request.referenceNumber());
        String note = Codes.blankToNull(request.note());

        // 1. batch lookups, then validate and normalize
        Map<UUID, ItemResponse> items = loadItems(request.lines());
        Map<UUID, LocationResponse> locations = loadLocations(request.lines());
        List<PreparedLine> lines = new ArrayList<>(request.lines().size());
        for (PostStockLine l : request.lines()) {
            lines.add(prepare(l, items, locations));
        }

        // 2. lots (creates a lot only when receiving into a new lot number)
        Map<LotKey, Lot> lotCache = resolveLots(lines);

        // 3. lock existing balances in a deterministic order
        Map<BalanceKey, StockBalance> locked = lockBalances(lines, lotCache);

        // 4. apply in submitted order
        List<MovementResponse> result = new ArrayList<>(lines.size());
        for (PreparedLine p : lines) {
            BalanceKey key = keyOf(p, lotCache);
            if (p.serial() != null && p.delta().signum() > 0) {
                assertSerialNotInStock(p);
            }
            StockBalance balance = locked.get(key);
            if (balance == null) {
                if (p.delta().signum() < 0) {
                    throw insufficient(p, BigDecimal.ZERO);
                }
                balance = new StockBalance();
                balance.setWarehouseId(p.location().warehouseId());
                balance.setLocationId(p.location().id());
                balance.setItemId(p.item().id());
                balance.setLot(p.lotNumber() == null ? null : lotCache.get(new LotKey(p.item().id(), p.lotNumber())));
                balance.setSerialNumber(p.serial());
                balance.setQuantity(BigDecimal.ZERO);
                balances.save(balance);
                locked.put(key, balance);
            }
            BigDecimal newQty = balance.getQuantity().add(p.delta());
            if (newQty.signum() < 0) {
                throw insufficient(p, balance.getQuantity());
            }
            balance.setQuantity(newQty);

            StockMovement m = new StockMovement();
            m.setTransactionId(transactionId);
            m.setType(p.type());
            m.setOccurredAt(occurredAt);
            m.setWarehouseId(p.location().warehouseId());
            m.setLocationId(p.location().id());
            m.setItemId(p.item().id());
            m.setLot(balance.getLot());
            m.setSerialNumber(p.serial());
            m.setQuantity(p.delta());
            m.setBalanceAfter(newQty);
            m.setUnitCost(p.unitCost());                 // exactly what the caller sent; null stays null
            m.setReferenceType(referenceType);
            m.setReferenceId(request.referenceId());
            m.setReferenceNumber(referenceNumber);
            m.setNote(note);
            movements.save(m);
            result.add(MovementMapper.toResponse(m));
        }

        // 5. surface unique / check constraint violations here (409), not at commit
        balances.flush();
        return new PostStockResponse(transactionId, result);
    }

    @Override
    @Transactional(readOnly = true)
    public void check(PostStockRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessException("STOCK_NO_LINES", "At least one stock line is required");
        }
        Map<UUID, ItemResponse> items = loadItems(request.lines());
        Map<UUID, LocationResponse> locations = loadLocations(request.lines());
        List<PreparedLine> lines = new ArrayList<>(request.lines().size());
        for (PostStockLine l : request.lines()) {
            lines.add(prepare(l, items, locations));
        }

        // existing lots only: nothing is created in a dry run
        Map<UUID, Set<String>> numbersByItem = new HashMap<>();
        for (PreparedLine p : lines) {
            if (p.lotNumber() != null) {
                numbersByItem.computeIfAbsent(p.item().id(), k -> new HashSet<>()).add(p.lotNumber());
            }
        }
        Map<LotKey, Lot> existing = new HashMap<>();
        numbersByItem.forEach((itemId, numbers) ->
                lots.findByItemIdAndLotNumberIn(itemId, numbers)
                        .forEach(lot -> existing.put(new LotKey(itemId, lot.getLotNumber()), lot)));

        // run the lines in order against an in-memory copy of the quantities
        Map<CheckKey, BigDecimal> simulated = new HashMap<>();
        for (PreparedLine p : lines) {
            Lot lot = p.lotNumber() == null ? null : existing.get(new LotKey(p.item().id(), p.lotNumber()));
            assertLotRules(p, lot);
            CheckKey key = new CheckKey(p.location().id(), p.item().id(), p.lotNumber(), p.serial());
            BigDecimal current = simulated.containsKey(key) ? simulated.get(key) : readQuantity(p, lot);
            BigDecimal next = current.add(p.delta());
            if (next.signum() < 0) {
                throw insufficient(p, current);
            }
            simulated.put(key, next);
        }
    }

    // ------------------------------------------------------------------ lookups

    private Map<UUID, ItemResponse> loadItems(List<PostStockLine> lines) {
        Set<UUID> ids = new HashSet<>();
        for (PostStockLine l : lines) {
            if (l == null || l.itemId() == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item is required on every line");
            }
            ids.add(l.itemId());
        }
        return itemService.findAllByIds(ids).stream()
                .collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    private Map<UUID, LocationResponse> loadLocations(List<PostStockLine> lines) {
        Map<UUID, LocationResponse> map = new HashMap<>();
        for (PostStockLine l : lines) {
            if (l.locationId() == null) {
                throw new BusinessException("LOCATION_NOT_FOUND", "Location is required on every line");
            }
            // BusinessException unless an active BIN in an active warehouse; one call per distinct bin
            map.computeIfAbsent(l.locationId(), locationService::requireStorableBin);
        }
        return map;
    }

    // ------------------------------------------------------------------ validation

    private PreparedLine prepare(PostStockLine l, Map<UUID, ItemResponse> items, Map<UUID, LocationResponse> locations) {
        if (l.type() == null) {
            throw new BusinessException("MOVEMENT_TYPE_REQUIRED", "Movement type is required");
        }
        ItemResponse item = items.get(l.itemId());
        if (item == null) {
            throw new BusinessException("ITEM_NOT_FOUND", "Item not found: " + l.itemId());
        }
        if (l.unitCost() != null && (l.unitCost().signum() < 0
                || l.unitCost().stripTrailingZeros().scale() > StockLineRules.SCALE)) {
            throw new BusinessException("COST_INVALID", "Unit cost must be zero or more with at most 4 decimals");
        }
        String lot = InventoryCodes.normalizeOrNull(l.lotNumber());
        String serial = InventoryCodes.normalizeOrNull(l.serialNumber());
        StockLineRules.check(l.type(), item, l.quantity(), lot, l.expiryDate(), serial);
        return new PreparedLine(l.type(), locations.get(l.locationId()), item, lot, l.expiryDate(), serial,
                l.quantity(), l.unitCost());
    }

    // ------------------------------------------------------------------ lots

    private Map<LotKey, Lot> resolveLots(List<PreparedLine> lines) {
        Map<UUID, Set<String>> numbersByItem = new HashMap<>();
        for (PreparedLine p : lines) {
            if (p.lotNumber() != null) {
                numbersByItem.computeIfAbsent(p.item().id(), k -> new HashSet<>()).add(p.lotNumber());
            }
        }
        Map<LotKey, Lot> cache = new HashMap<>();
        numbersByItem.forEach((itemId, numbers) ->
                lots.findByItemIdAndLotNumberIn(itemId, numbers)
                        .forEach(lot -> cache.put(new LotKey(itemId, lot.getLotNumber()), lot)));

        for (PreparedLine p : lines) {
            if (p.lotNumber() == null) {
                continue;
            }
            LotKey key = new LotKey(p.item().id(), p.lotNumber());
            Lot lot = cache.get(key);
            assertLotRules(p, lot);
            if (lot == null) {                       // rules passed, so this is a positive delta: create the lot
                lot = new Lot();
                lot.setItemId(p.item().id());
                lot.setLotNumber(p.lotNumber());
                lot.setExpiryDate(p.expiryDate());
                lots.save(lot);
                cache.put(key, lot);
            }
        }
        return cache;
    }

    // ------------------------------------------------------------------ balances

    private BalanceKey keyOf(PreparedLine p, Map<LotKey, Lot> lotCache) {
        UUID lotId = p.lotNumber() == null ? null
                : lotCache.get(new LotKey(p.item().id(), p.lotNumber())).getId();
        return new BalanceKey(p.location().id(), p.item().id(), lotId, p.serial());
    }

    private Map<BalanceKey, StockBalance> lockBalances(List<PreparedLine> lines, Map<LotKey, Lot> lotCache) {
        List<BalanceKey> keys = lines.stream().map(p -> keyOf(p, lotCache)).distinct().sorted(KEY_ORDER).toList();
        Map<BalanceKey, StockBalance> locked = new HashMap<>();
        for (BalanceKey k : keys) {
            Optional<StockBalance> row;
            if (k.serial() != null) {
                row = balances.lockSerial(k.locationId(), k.itemId(), k.serial());
            } else if (k.lotId() != null) {
                row = balances.lockLot(k.locationId(), k.itemId(), k.lotId());
            } else {
                row = balances.lockUntracked(k.locationId(), k.itemId());
            }
            row.ifPresent(b -> locked.put(k, b));
        }
        return locked;
    }

    private void assertSerialNotInStock(PreparedLine p) {
        balances.findByItemIdAndSerialNumberAndQuantityGreaterThan(p.item().id(), p.serial(), BigDecimal.ZERO)
                .ifPresent(b -> {
                    throw new ConflictException("SERIAL_ALREADY_IN_STOCK",
                            "Serial " + p.serial() + " of item " + p.item().sku() + " is already in stock");
                });
    }

    private BusinessException insufficient(PreparedLine p, BigDecimal available) {
        return new BusinessException("INSUFFICIENT_STOCK",
                "Insufficient stock of %s at %s: available %s, requested %s".formatted(
                        p.item().sku(), p.location().code(),
                        available.stripTrailingZeros().toPlainString(),
                        p.delta().abs().stripTrailingZeros().toPlainString()));
    }

    private BigDecimal readQuantity(PreparedLine p, Lot lot) {
        UUID loc = p.location().id();
        UUID item = p.item().id();
        Optional<BigDecimal> q;
        if (p.serial() != null) {
            q = balances.quantitySerial(loc, item, p.serial());
        } else if (p.lotNumber() != null) {
            q = lot == null ? Optional.empty() : balances.quantityLot(loc, item, lot.getId());
        } else {
            q = balances.quantityUntracked(loc, item);
        }
        return q.orElse(BigDecimal.ZERO);
    }

    /** Lot and expiry rules, shared by post and check. lot is null when the lot number does not exist yet. */
    private void assertLotRules(PreparedLine p, Lot lot) {
        if (p.lotNumber() == null) {
            return;
        }
        if (lot == null) {
            if (p.delta().signum() < 0) {
                throw new BusinessException("LOT_NOT_FOUND",
                        "Lot " + p.lotNumber() + " does not exist for item " + p.item().sku());
            }
            if (p.item().expiryTracked() && p.expiryDate() == null) {
                throw new BusinessException("EXPIRY_REQUIRED",
                        "Expiry date required for new lot " + p.lotNumber() + " of item " + p.item().sku());
            }
        } else if (p.expiryDate() != null && !p.expiryDate().equals(lot.getExpiryDate())) {
            throw new BusinessException("LOT_EXPIRY_MISMATCH",
                    "Lot " + p.lotNumber() + " of item " + p.item().sku() + " already has expiry "
                            + lot.getExpiryDate() + ", received " + p.expiryDate());
        }
    }
}

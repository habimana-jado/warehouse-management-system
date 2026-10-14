package rw.afriteck.wms.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.support.Codes;
import rw.afriteck.wms.inventory.dtos.MovementFilter;
import rw.afriteck.wms.inventory.dtos.MovementHistoryResponse;
import rw.afriteck.wms.inventory.dtos.StockFilter;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.entity.StockBalance;
import rw.afriteck.wms.inventory.entity.StockMovement;
import rw.afriteck.wms.inventory.mapper.MovementMapper;
import rw.afriteck.wms.inventory.mapper.StockLevelMapper;
import rw.afriteck.wms.inventory.repository.StockBalanceRepository;
import rw.afriteck.wms.inventory.repository.StockMovementRepository;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.inventory.support.InventoryCodes;
import rw.afriteck.wms.inventory.support.MovementSpecs;
import rw.afriteck.wms.inventory.support.PageRules;
import rw.afriteck.wms.inventory.support.StockBalanceSpecs;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {

    private static final int CHUNK = 1000;     // keeps IN lists far below PostgreSQL's bind-parameter limit
    private static final Set<String> STOCK_SORTS = Set.of("quantity", "updatedAt", "createdAt");
    private static final Set<String> MOVEMENT_SORTS = Set.of("occurredAt", "quantity", "createdAt");

    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final ItemService itemService;
    private final LocationService locationService;

    // ------------------------------------------------------------ cross-module reads

    @Override
    public List<StockLevelResponse> findAvailable(UUID warehouseId, UUID itemId) {
        List<StockBalance> rows = balances.findAvailable(warehouseId, itemId);
        return map(rows);
    }

    @Override
    public BigDecimal getOnHand(UUID warehouseId, UUID itemId) {
        return balances.sumQuantity(warehouseId, itemId);
    }

    @Override
    public Map<UUID, BigDecimal> getOnHand(UUID warehouseId, Collection<UUID> itemIds) {
        Map<UUID, BigDecimal> result = zeroed(itemIds);
        for (List<UUID> chunk : partition(result.keySet())) {
            balances.sumByItems(warehouseId, chunk)
                    .forEach(r -> result.put(r.getItemId(), r.getQuantity()));
        }
        return result;
    }

    @Override
    public Map<UUID, BigDecimal> getAvailableInStorage(UUID warehouseId, Collection<UUID> itemIds) {
        Map<UUID, BigDecimal> result = zeroed(itemIds);
        if (result.isEmpty()) {
            return result;
        }
        List<UUID> binIds = locationService.findBinsByUsage(warehouseId, LocationUsage.STORAGE).stream()
                .map(LocationResponse::id).toList();
        for (List<UUID> itemChunk : partition(result.keySet())) {
            for (List<UUID> binChunk : partition(binIds)) {
                balances.sumByItemsInLocations(binChunk, itemChunk)
                        .forEach(r -> result.merge(r.getItemId(), r.getQuantity(), BigDecimal::add));
            }
        }
        return result;
    }

    // ------------------------------------------------------------ screens

    @Override
    public PageResponse<StockLevelResponse> search(StockFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        StockFilter n = new StockFilter(f.warehouseId(), f.locationId(), f.itemId(), f.lotId(),
                InventoryCodes.normalizeOrNull(f.serialNumber()), f.includeZero());
        Pageable p = PageRules.sanitize(pageable, STOCK_SORTS, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<StockBalance> page = balances.findAll(StockBalanceSpecs.of(n), p);
        Function<StockBalance, StockLevelResponse> mapper = levelMapper(page.getContent());
        return PageResponse.of(page.map(mapper));
    }

    @Override
    public List<StockLevelResponse> findInLocation(UUID warehouseId, UUID locationId) {
        LocationResponse loc = locationService.getById(locationId);
        if (!loc.warehouseId().equals(warehouseId)) {
            throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Location belongs to another warehouse");
        }
        return map(balances.findInLocation(locationId));
    }

    @Override
    public PageResponse<MovementHistoryResponse> history(MovementFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        if (f.from() != null && f.to() != null && !f.from().isBefore(f.to())) {
            throw new BusinessException("INVALID_DATE_RANGE", "'from' must be before 'to'");
        }
        String refType = f.referenceType() == null ? null : Codes.blankToNull(Codes.normalize(f.referenceType()));
        MovementFilter n = new MovementFilter(f.warehouseId(), f.itemId(), f.locationId(), f.lotId(),
                InventoryCodes.normalizeOrNull(f.serialNumber()), f.types(), f.from(), f.to(), refType, f.referenceId());
        Pageable p = PageRules.sanitize(pageable, MOVEMENT_SORTS, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<StockMovement> page = movements.findAll(MovementSpecs.of(n), p);

        Map<UUID, ItemResponse> items = itemsById(page.getContent().stream().map(StockMovement::getItemId).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locs = locationsById(page.getContent().stream().map(StockMovement::getLocationId).collect(Collectors.toSet()));
        return PageResponse.of(page.map(m ->
                MovementMapper.toHistory(m, items.get(m.getItemId()), locs.get(m.getLocationId()))));
    }

    // ------------------------------------------------------------ helpers

    private List<StockLevelResponse> map(List<StockBalance> rows) {
        Function<StockBalance, StockLevelResponse> mapper = levelMapper(rows);
        return rows.stream().map(mapper).toList();
    }

    private Function<StockBalance, StockLevelResponse> levelMapper(Collection<StockBalance> rows) {
        Map<UUID, ItemResponse> items = itemsById(rows.stream().map(StockBalance::getItemId).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locs = locationsById(rows.stream().map(StockBalance::getLocationId).collect(Collectors.toSet()));
        return b -> StockLevelMapper.toResponse(b, items.get(b.getItemId()), locs.get(b.getLocationId()));
    }

    private Map<UUID, ItemResponse> itemsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return itemService.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    private Map<UUID, LocationResponse> locationsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return locationService.findAllByIds(ids).stream().collect(Collectors.toMap(LocationResponse::id, Function.identity()));
    }

    private static Map<UUID, BigDecimal> zeroed(Collection<UUID> itemIds) {
        Map<UUID, BigDecimal> m = new HashMap<>();
        if (itemIds != null) itemIds.forEach(id -> m.put(id, BigDecimal.ZERO));
        return m;
    }

    private static List<List<UUID>> partition(Collection<UUID> ids) {
        List<UUID> all = new ArrayList<>(ids);
        List<List<UUID>> chunks = new ArrayList<>();
        for (int i = 0; i < all.size(); i += CHUNK) {
            chunks.add(all.subList(i, Math.min(i + CHUNK, all.size())));
        }
        return chunks;
    }
}

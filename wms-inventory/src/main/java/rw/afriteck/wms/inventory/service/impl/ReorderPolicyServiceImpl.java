package rw.afriteck.wms.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.CreateReorderPolicyRequest;
import rw.afriteck.wms.inventory.dtos.LowStockResponse;
import rw.afriteck.wms.inventory.dtos.ReorderPolicyResponse;
import rw.afriteck.wms.inventory.dtos.UpdateReorderPolicyRequest;
import rw.afriteck.wms.inventory.entity.ReorderPolicy;
import rw.afriteck.wms.inventory.mapper.ReorderPolicyMapper;
import rw.afriteck.wms.inventory.repository.ReorderPolicyRepository;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.inventory.service.ReorderPolicyService;
import rw.afriteck.wms.inventory.support.PageRules;
import rw.afriteck.wms.inventory.support.StockLineRules;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReorderPolicyServiceImpl implements ReorderPolicyService {

    private static final Set<String> SORTS = Set.of("createdAt", "updatedAt");

    private final ReorderPolicyRepository policies;
    private final InventoryQueryService queries;
    private final WarehouseService warehouseService;
    private final ItemService itemService;

    @Override
    @Transactional
    public ReorderPolicyResponse create(CreateReorderPolicyRequest r) {
        if (r == null || r.warehouseId() == null || r.itemId() == null) {
            throw new BusinessException("REORDER_FIELDS_REQUIRED", "Warehouse and item are required");
        }
        if (!warehouseService.getById(r.warehouseId()).active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        ItemResponse item = itemService.getById(r.itemId());
        if (!item.active()) {
            throw new BusinessException("ITEM_INACTIVE", "Item is inactive: " + item.sku());
        }
        validateLevels(r.minQty(), r.reorderQty(), r.maxQty());
        if (policies.existsByWarehouseIdAndItemId(r.warehouseId(), r.itemId())) {
            throw new ConflictException("REORDER_POLICY_EXISTS", "A reorder policy already exists for this item in the warehouse");
        }
        ReorderPolicy p = new ReorderPolicy();
        p.setWarehouseId(r.warehouseId());
        p.setItemId(r.itemId());
        p.setMinQty(r.minQty());
        p.setReorderQty(r.reorderQty());
        p.setMaxQty(r.maxQty());
        return ReorderPolicyMapper.toResponse(policies.saveAndFlush(p), item);
    }

    @Override
    @Transactional
    public ReorderPolicyResponse update(UUID id, UpdateReorderPolicyRequest r) {
        ReorderPolicy p = load(id);
        BigDecimal min = r.minQty() != null ? r.minQty() : p.getMinQty();
        BigDecimal reorder = r.reorderQty() != null ? r.reorderQty() : p.getReorderQty();
        BigDecimal max = Boolean.TRUE.equals(r.clearMaxQty()) ? null : (r.maxQty() != null ? r.maxQty() : p.getMaxQty());
        validateLevels(min, reorder, max);
        p.setMinQty(min);
        p.setReorderQty(reorder);
        p.setMaxQty(max);
        if (r.active() != null) {
            p.setActive(r.active());
        }
        return ReorderPolicyMapper.toResponse(policies.saveAndFlush(p), itemService.getById(p.getItemId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReorderPolicyResponse getById(UUID id) {
        ReorderPolicy p = load(id);
        return ReorderPolicyMapper.toResponse(p, itemService.getById(p.getItemId()));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID warehouseIdOf(UUID id) {
        return load(id).getWarehouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReorderPolicyResponse> search(UUID warehouseId, boolean activeOnly, Pageable pageable) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        Pageable p = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ReorderPolicy> page = activeOnly
                ? policies.findByWarehouseIdAndActive(warehouseId, true, p)
                : policies.findByWarehouseId(warehouseId, p);
        Map<UUID, ItemResponse> items = itemsById(page.getContent().stream()
                .map(ReorderPolicy::getItemId).collect(Collectors.toSet()));
        return PageResponse.of(page.map(x -> ReorderPolicyMapper.toResponse(x, items.get(x.getItemId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockResponse> findBelowMinimum(UUID warehouseId) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        List<ReorderPolicy> active = policies.findByWarehouseIdAndActiveTrue(warehouseId);
        if (active.isEmpty()) {
            return List.of();
        }
        Set<UUID> itemIds = active.stream().map(ReorderPolicy::getItemId).collect(Collectors.toSet());
        Map<UUID, BigDecimal> onHand = queries.getAvailableInStorage(warehouseId, itemIds);   // STORAGE bins only
        Map<UUID, ItemResponse> items = itemsById(itemIds);

        List<LowStockResponse> result = new ArrayList<>();
        for (ReorderPolicy p : active) {
            ItemResponse item = items.get(p.getItemId());
            if (item == null || !item.active()) {
                continue;                                         // a policy left behind on a deactivated item
            }
            BigDecimal qty = onHand.getOrDefault(p.getItemId(), BigDecimal.ZERO);
            if (qty.compareTo(p.getMinQty()) > 0) {
                continue;
            }
            result.add(new LowStockResponse(item.id(), item.sku(), item.name(), item.baseUom(),
                    qty, p.getMinQty(), p.getReorderQty(), p.getMaxQty(), suggested(p, qty)));
        }
        result.sort(Comparator.comparing(x -> x.onHand().subtract(x.minQty())));          // furthest below minimum first
        return result;
    }

    private static BigDecimal suggested(ReorderPolicy p, BigDecimal onHand) {
        BigDecimal qty = p.getReorderQty();
        if (p.getMaxQty() != null) {
            qty = qty.min(p.getMaxQty().subtract(onHand).max(BigDecimal.ZERO));
        }
        return qty;
    }

    private static void validateLevels(BigDecimal min, BigDecimal reorder, BigDecimal max) {
        if (min == null || min.signum() < 0 || badScale(min)
                || reorder == null || reorder.signum() <= 0 || badScale(reorder)
                || (max != null && badScale(max))) {
            throw new BusinessException("REORDER_QTY_INVALID",
                    "minQty must be zero or more, reorderQty above zero, all with at most 4 decimals");
        }
        if (max != null && max.compareTo(min) <= 0) {
            throw new BusinessException("REORDER_LEVELS_INVALID", "maxQty must be above minQty");
        }
    }

    private static boolean badScale(BigDecimal q) {
        return q.stripTrailingZeros().scale() > StockLineRules.SCALE;
    }

    private ReorderPolicy load(UUID id) {
        return policies.findById(id).orElseThrow(() -> new NotFoundException("ReorderPolicy", id));
    }

    private Map<UUID, ItemResponse> itemsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return itemService.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }
}

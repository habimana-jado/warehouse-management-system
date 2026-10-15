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
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;
import rw.afriteck.wms.inventory.entity.StockAdjustment;
import rw.afriteck.wms.inventory.entity.StockAdjustmentLine;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.mapper.AdjustmentMapper;
import rw.afriteck.wms.inventory.repository.AdjustmentReasonRepository;
import rw.afriteck.wms.inventory.repository.StockAdjustmentRepository;
import rw.afriteck.wms.inventory.service.AdjustmentService;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.inventory.support.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdjustmentServiceImpl implements AdjustmentService {

    private static final Set<String> SORTS = Set.of("createdAt", "adjustmentNumber", "status");

    private final StockAdjustmentRepository adjustments;
    private final AdjustmentReasonRepository reasons;
    private final InventoryService inventory;
    private final WarehouseService warehouseService;
    private final ItemService itemService;
    private final LocationService locationService;
    private final WorkerLookup workers;
    private final Clock clock;

    // ------------------------------------------------------------------ create

    @Override
    @Transactional
    public AdjustmentResponse create(CreateAdjustmentRequest r, UUID requesterId) {
        requireActor(requesterId);
        if (r == null || r.lines() == null || r.lines().isEmpty()) {
            throw new BusinessException("ADJUSTMENT_NO_LINES", "At least one line is required");
        }
        WarehouseResponse warehouse = warehouseService.getById(r.warehouseId());
        if (!warehouse.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse is inactive");
        }
        AdjustmentReason reason = reasons.findById(r.reasonId())
                .orElseThrow(() -> new NotFoundException("AdjustmentReason", r.reasonId()));
        if (!reason.isActive()) {
            throw new BusinessException("REASON_INACTIVE", "Reason is inactive: " + reason.getCode());
        }

        Map<UUID, ItemResponse> items = itemsById(r.lines().stream()
                .map(AdjustmentLineRequest::itemId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<UUID, LocationResponse> locations = new HashMap<>();

        StockAdjustment adj = new StockAdjustment();
        adj.setWarehouseId(warehouse.id());
        adj.setReason(reason);
        adj.setRequestedBy(requesterId);
        adj.setNotes(Codes.blankToNull(r.notes()));
        adj.setStatus(AdjustmentStatus.PENDING_APPROVAL);

        int lineNo = 1;
        for (AdjustmentLineRequest l : r.lines()) {
            ItemResponse item = l.itemId() == null ? null : items.get(l.itemId());
            if (item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item not found: " + l.itemId());
            }
            if (l.locationId() == null) {
                throw new BusinessException("LOCATION_NOT_FOUND", "Location is required on every line");
            }
            LocationResponse loc = locations.computeIfAbsent(l.locationId(), locationService::requireStorableBin);
            if (!loc.warehouseId().equals(warehouse.id())) {
                throw new BusinessException("LOCATION_WRONG_WAREHOUSE",
                        "Location " + loc.code() + " belongs to another warehouse");
            }
            String lot = InventoryCodes.normalizeOrNull(l.lotNumber());
            String serial = InventoryCodes.normalizeOrNull(l.serialNumber());
            StockLineRules.check(MovementType.ADJUSTMENT, item, l.quantityDelta(), lot, l.expiryDate(), serial);
            checkDirection(reason, l.quantityDelta());

            StockAdjustmentLine line = new StockAdjustmentLine();
            line.setAdjustment(adj);
            line.setLineNo(lineNo++);
            line.setLocationId(loc.id());
            line.setItemId(item.id());
            line.setLotNumber(lot);
            line.setExpiryDate(l.expiryDate());
            line.setSerialNumber(serial);
            line.setQuantityDelta(l.quantityDelta());
            adj.getLines().add(line);
        }

        inventory.check(new PostStockRequest(null, null, null, null, null, null,
                adj.getLines().stream()
                        .map(l -> new PostStockLine(MovementType.ADJUSTMENT, l.getLocationId(), l.getItemId(),
                                l.getLotNumber(), l.getExpiryDate(), l.getSerialNumber(), l.getQuantityDelta(), null))
                        .toList()));

        adj.setAdjustmentNumber(InventoryNumbers.adjustment(adjustments.nextNumber()));
        adjustments.save(adj);                                  // assigns the id used as the posting's transactionId

        if (!reason.isRequiresApproval()) {                     // no approval needed: post right away
            postLines(adj);
            adj.setStatus(AdjustmentStatus.POSTED);
            adj.setDecidedAt(clock.instant());                  // decidedBy stays null: nobody decided
        }
        return toResponse(adjustments.saveAndFlush(adj));
    }

    // ------------------------------------------------------------------ decisions

    @Override
    @Transactional
    public AdjustmentResponse approve(UUID id, UUID approverId) {
        requireActor(approverId);
        StockAdjustment adj = load(id);
        requirePending(adj);
        if (approverId.equals(adj.getRequestedBy())) {
            throw new BusinessException("SELF_APPROVAL_NOT_ALLOWED", "You cannot approve your own adjustment");
        }
        postLines(adj);                                         // INSUFFICIENT_STOCK etc. roll everything back
        adj.setStatus(AdjustmentStatus.POSTED);
        adj.setDecidedBy(approverId);
        adj.setDecidedAt(clock.instant());
        return toResponse(adjustments.saveAndFlush(adj));
    }

    @Override
    @Transactional
    public AdjustmentResponse reject(UUID id, UUID rejecterId, String reason) {
        requireActor(rejecterId);
        String text = Codes.blankToNull(reason);
        if (text == null) {
            throw new BusinessException("REJECTION_REASON_REQUIRED", "A rejection reason is required");
        }
        StockAdjustment adj = load(id);
        requirePending(adj);
        adj.setStatus(AdjustmentStatus.REJECTED);
        adj.setDecidedBy(rejecterId);
        adj.setDecidedAt(clock.instant());
        adj.setRejectionReason(text);
        return toResponse(adjustments.saveAndFlush(adj));
    }

    @Override
    @Transactional
    public AdjustmentResponse cancel(UUID id, UUID actorId) {
        requireActor(actorId);
        StockAdjustment adj = load(id);
        requirePending(adj);
        if (!actorId.equals(adj.getRequestedBy())) {
            throw new ForbiddenException("Only the requester can cancel an adjustment");
        }
        adj.setStatus(AdjustmentStatus.CANCELLED);
        adj.setDecidedAt(clock.instant());
        return toResponse(adjustments.saveAndFlush(adj));
    }

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public AdjustmentResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public AdjustmentAccessInfo accessInfo(UUID id) {
        return adjustments.findById(id)
                .map(a -> new AdjustmentAccessInfo(a.getWarehouseId(), a.getRequestedBy()))
                .orElseThrow(() -> new NotFoundException("StockAdjustment", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdjustmentSummaryResponse> search(AdjustmentFilter f, Pageable pageable) {
        if (f == null || f.warehouseId() == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "warehouseId is required");
        }
        Set<AdjustmentStatus> statuses = (f.statuses() == null || f.statuses().isEmpty())
                ? EnumSet.allOf(AdjustmentStatus.class) : f.statuses();       // never an empty IN list
        Pageable p = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "createdAt"));

        String pattern = Codes.likePattern(f.q());
        Page<StockAdjustment> page = f.requestedBy() == null
                ? adjustments.search(f.warehouseId(), statuses, pattern, p)
                : adjustments.searchByRequester(f.warehouseId(), f.requestedBy(), statuses, pattern, p);

        Map<UUID, Integer> counts = new HashMap<>();
        List<UUID> ids = page.getContent().stream().map(StockAdjustment::getId).toList();
        if (!ids.isEmpty()) {
            adjustments.countLines(ids).forEach(c -> counts.put(c.getAdjustmentId(), (int) c.getLineCount()));
        }
        Map<UUID, String> names = workers.findDisplayNames(page.getContent().stream()
                .map(StockAdjustment::getRequestedBy).collect(Collectors.toSet()));
        return PageResponse.of(page.map(a ->
                AdjustmentMapper.toSummary(a, counts.getOrDefault(a.getId(), 0), names)));
    }

    // ------------------------------------------------------------------ helpers

    private void postLines(StockAdjustment adj) {
        List<PostStockLine> lines = adj.getLines().stream()
                .map(l -> new PostStockLine(MovementType.ADJUSTMENT, l.getLocationId(), l.getItemId(),
                        l.getLotNumber(), l.getExpiryDate(), l.getSerialNumber(), l.getQuantityDelta(), null))
                .toList();
        inventory.post(new PostStockRequest(adj.getId(), null, InventoryReferences.STOCK_ADJUSTMENT,
                adj.getId(), adj.getAdjustmentNumber(), adj.getReason().getCode(), lines));
    }

    private StockAdjustment load(UUID id) {
        return adjustments.findWithLinesById(id).orElseThrow(() -> new NotFoundException("StockAdjustment", id));
    }

    private static void requireActor(UUID actorId) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "A user is required for this operation");
        }
    }

    private static void requirePending(StockAdjustment adj) {
        if (adj.getStatus() != AdjustmentStatus.PENDING_APPROVAL) {
            throw new ConflictException("ADJUSTMENT_INVALID_STATE",
                    "Adjustment " + adj.getAdjustmentNumber() + " is " + adj.getStatus());
        }
    }

    private static void checkDirection(AdjustmentReason reason, BigDecimal delta) {
        boolean ok = switch (reason.getDirection()) {
            case INCREASE -> delta.signum() > 0;
            case DECREASE -> delta.signum() < 0;
            case BOTH -> true;
        };
        if (!ok) {
            throw new BusinessException("ADJUSTMENT_DIRECTION_INVALID",
                    "Reason " + reason.getCode() + " only allows " + reason.getDirection());
        }
    }

    private AdjustmentResponse toResponse(StockAdjustment a) {
        Set<UUID> userIds = new HashSet<>();
        userIds.add(a.getRequestedBy());
        if (a.getDecidedBy() != null) userIds.add(a.getDecidedBy());
        Set<UUID> itemIds = a.getLines().stream().map(StockAdjustmentLine::getItemId).collect(Collectors.toSet());
        Set<UUID> locationIds = a.getLines().stream().map(StockAdjustmentLine::getLocationId).collect(Collectors.toSet());
        return AdjustmentMapper.toResponse(a, workers.findDisplayNames(userIds),
                itemsById(itemIds), locationsById(locationIds));
    }

    private Map<UUID, ItemResponse> itemsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return itemService.findAllByIds(ids).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));
    }

    private Map<UUID, LocationResponse> locationsById(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return locationService.findAllByIds(ids).stream().collect(Collectors.toMap(LocationResponse::id, Function.identity()));
    }
}

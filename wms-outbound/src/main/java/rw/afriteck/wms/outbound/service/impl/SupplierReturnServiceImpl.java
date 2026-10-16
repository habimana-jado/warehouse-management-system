package rw.afriteck.wms.outbound.service.impl;

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
import rw.afriteck.wms.common.support.Chunks;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.*;
import rw.afriteck.wms.inventory.dtos.PostStockLine;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.enums.MovementType;
import rw.afriteck.wms.inventory.service.InventoryService;
import rw.afriteck.wms.outbound.dtos.*;
import rw.afriteck.wms.outbound.entity.*;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import rw.afriteck.wms.outbound.mappers.SupplierReturnMapper;
import rw.afriteck.wms.outbound.repository.SupplierReturnRepository;
import rw.afriteck.wms.outbound.service.SupplierReturnService;
import rw.afriteck.wms.outbound.support.OutboundNumbers;
import rw.afriteck.wms.outbound.support.OutboundReferences;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierReturnServiceImpl implements SupplierReturnService {

    private static final Set<String> SORTS = Set.of("returnDate", "returnNumber", "status", "createdAt");

    private final SupplierReturnRepository returns;
    private final SupplierService suppliers;
    private final WarehouseService warehouses;
    private final ItemService items;
    private final LocationService locations;
    private final InventoryService inventory;
    private final OutboundNumbers numbers;
    private final Clock clock;

    private record PreparedLine(UUID itemId, UUID locationId, String lot, String serial, BigDecimal qty, String note) {}

    // ------------------------------------------------------------------ reads

    @Override
    @Transactional(readOnly = true)
    public SupplierReturnResponse getById(UUID id) {
        return toResponse(load(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UUID warehouseIdOf(UUID id) {
        return load(id).getWarehouseId();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierReturnSummaryResponse> search(UUID warehouseId, Set<SupplierReturnStatus> statuses,
                                                              String q, Pageable pageable) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
        Set<SupplierReturnStatus> wanted = (statuses == null || statuses.isEmpty())
                ? EnumSet.allOf(SupplierReturnStatus.class) : statuses;       // never an empty IN list
        String pattern = q == null || q.isBlank() ? "%" : Codes.likePattern(q);
        Pageable safe = PageRules.sanitize(pageable, SORTS, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupplierReturn> page = returns.search(warehouseId, wanted, pattern, safe);

        List<UUID> ids = page.getContent().stream().map(SupplierReturn::getId).toList();
        Map<UUID, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (List<UUID> chunk : Chunks.partition(ids)) {
                returns.countLines(chunk).forEach(c -> counts.put(c.getParentId(), c.getTotal()));
            }
        }
        Map<UUID, String> names = new HashMap<>();
        return PageResponse.of(page.map(r -> SupplierReturnMapper.toSummary(r,
                names.computeIfAbsent(r.getSupplierId(), id -> suppliers.getById(id).name()),
                counts.getOrDefault(r.getId(), 0L).intValue())));
    }

    // ----------------------------------------------------------------- create

    @Override
    public SupplierReturnResponse create(CreateSupplierReturnRequest r) {
        String reason = r == null ? null : Codes.blankToNull(r.reason());
        if (r == null || r.supplierId() == null || r.warehouseId() == null || reason == null) {
            throw new BusinessException("RTV_FIELDS_REQUIRED", "Supplier, warehouse and reason are required");
        }
        String supplierReference = Codes.blankToNull(r.supplierReference());
        String notes = Codes.blankToNull(r.notes());
        checkLength(reason, 500, "Reason");
        checkLength(supplierReference, 60, "Supplier reference");
        checkLength(notes, 1000, "Notes");

        SupplierResponse supplier = suppliers.getById(r.supplierId());
        if (!supplier.active()) {
            throw new BusinessException("SUPPLIER_INACTIVE", "The supplier is inactive");
        }
        if (!warehouses.getById(r.warehouseId()).active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "The warehouse is inactive");
        }
        List<PreparedLine> prepared = prepareLines(r.warehouseId(), r.lines());

        SupplierReturn rtv = new SupplierReturn();
        rtv.setReturnNumber(numbers.nextReturn());
        rtv.setSupplierId(r.supplierId());
        rtv.setWarehouseId(r.warehouseId());
        rtv.setStatus(SupplierReturnStatus.DRAFT);
        rtv.setReturnDate(r.returnDate() != null ? r.returnDate() : LocalDate.now(clock));
        rtv.setReason(reason);
        rtv.setSupplierReference(supplierReference);
        rtv.setNotes(notes);
        int lineNo = 1;
        for (PreparedLine p : prepared) {
            SupplierReturnLine l = new SupplierReturnLine();
            l.setLineNo(lineNo++);
            l.setItemId(p.itemId());
            l.setLocationId(p.locationId());
            l.setLotNumber(p.lot());
            l.setSerialNumber(p.serial());
            l.setQuantity(p.qty());
            l.setNote(p.note());
            rtv.addLine(l);
        }
        rtv = returns.saveAndFlush(rtv);
        inventory.check(toPostRequest(rtv));        // advisory early feedback (INSUFFICIENT_STOCK...); a failure rolls the draft back
        return toResponse(rtv);
    }

    /** Item, bin and tracking validation; quantities are converted to the item BASE UOM. */
    private List<PreparedLine> prepareLines(UUID warehouseId, List<SupplierReturnLineRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new BusinessException("RTV_NO_LINES", "A return needs at least one line");
        }
        Set<UUID> itemIds = new HashSet<>();
        Set<UUID> locationIds = new HashSet<>();
        for (SupplierReturnLineRequest l : requested) {
            if (l == null || l.itemId() == null || l.locationId() == null) {
                throw new BusinessException("RTV_FIELDS_REQUIRED", "Every line needs an item and a bin");
            }
            itemIds.add(l.itemId());
            locationIds.add(l.locationId());
        }
        Map<UUID, ItemResponse> itemMap = new HashMap<>();
        items.findAllByIds(itemIds).forEach(i -> itemMap.put(i.id(), i));
        Map<UUID, LocationResponse> bins = new HashMap<>();
        locations.findAllByIds(locationIds).forEach(b -> bins.put(b.id(), b));

        Set<String> serialKeys = new HashSet<>();
        List<PreparedLine> out = new ArrayList<>();
        for (SupplierReturnLineRequest l : requested) {
            // inactive items are allowed on purpose: inventory also lets negative deltas through for them
            ItemResponse item = itemMap.get(l.itemId());
            if (item == null) throw new BusinessException("ITEM_NOT_FOUND", "Item not found: " + l.itemId());
            LocationResponse bin = bins.get(l.locationId());
            if (bin == null) throw new BusinessException("LOCATION_NOT_FOUND", "Location not found: " + l.locationId());
            if (!bin.warehouseId().equals(warehouseId)) {
                throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Location " + bin.code() + " is in another warehouse");
            }
            if (bin.type() != LocationType.BIN || !bin.active()) {
                throw new BusinessException("LOCATION_NOT_STORABLE", "Location " + bin.code() + " is not an active bin");
            }
            if (bin.usage() != LocationUsage.STORAGE && bin.usage() != LocationUsage.DAMAGED) {
                throw new BusinessException("LOCATION_USAGE_INVALID", "Returns leave from STORAGE or DAMAGED bins: " + bin.code());
            }
            if (l.quantity() == null || l.quantity().signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Quantity must be greater than 0 for " + item.sku());
            }
            checkLength(l.note(), 255, "Line note");

            String lot = normalizeOrNull(l.lotNumber());
            String serial = normalizeOrNull(l.serialNumber());
            String uom = Codes.blankToNull(l.uom());
            BigDecimal qty = uom == null ? l.quantity()
                    : items.toBaseQuantity(l.itemId(), Codes.normalize(uom), l.quantity());
            if (qty.signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Quantity must be greater than 0 for " + item.sku());
            }
            switch (item.trackingType()) {
                case NONE -> {
                    if (lot != null || serial != null) {
                        throw new BusinessException("TRACKING_MISMATCH", item.sku() + " has no lot or serial tracking");
                    }
                }
                case LOT -> {
                    if (serial != null) throw new BusinessException("TRACKING_MISMATCH", item.sku() + " is lot tracked");
                    if (lot == null) throw new BusinessException("LOT_REQUIRED", "The lot of " + item.sku() + " is required");
                }
                case SERIAL -> {
                    if (lot != null) throw new BusinessException("TRACKING_MISMATCH", item.sku() + " is serial tracked");
                    if (serial == null) throw new BusinessException("SERIAL_REQUIRED", "The serial of " + item.sku() + " is required");
                    if (qty.compareTo(BigDecimal.ONE) != 0) {
                        throw new BusinessException("SERIAL_QTY_MUST_BE_ONE", "One unit per serial number");
                    }
                    if (!serialKeys.add(item.id() + "|" + serial)) {
                        throw new BusinessException("SERIAL_DUPLICATED", "Serial " + serial + " appears twice");
                    }
                }
            }
            out.add(new PreparedLine(l.itemId(), l.locationId(), lot, serial, qty, Codes.blankToNull(l.note())));
        }
        return out;
    }

    // ------------------------------------------------------------------- post

    /** Issues the goods out of the ledger. Lock order: the return row first, inventory balances second. */
    @Override
    public SupplierReturnResponse post(UUID id) {
        SupplierReturn rtv = loadForUpdate(id);
        requireDraft(rtv);
        inventory.post(toPostRequest(rtv));          // all lines or none; INSUFFICIENT_STOCK etc. pass through
        rtv.setStatus(SupplierReturnStatus.POSTED);
        rtv.setPostedAt(clock.instant());
        return toResponse(returns.saveAndFlush(rtv));
    }

    private PostStockRequest toPostRequest(SupplierReturn rtv) {
        List<PostStockLine> lines = rtv.getLines().stream()
                .map(l -> new PostStockLine(MovementType.ISSUE, l.getLocationId(), l.getItemId(), l.getLotNumber(), null,
                        l.getSerialNumber(), l.getQuantity().negate(), null))
                .toList();
        return new PostStockRequest(null, null, OutboundReferences.RTV, rtv.getId(), rtv.getReturnNumber(),
                rtv.getReason(), lines);
    }

    // ----------------------------------------------------------------- cancel

    @Override
    public SupplierReturnResponse cancel(UUID id, CancelSupplierReturnRequest r) {
        String reason = r == null ? null : Codes.blankToNull(r.reason());
        if (reason == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancel reason is required");
        }
        checkLength(reason, 500, "Reason");
        SupplierReturn rtv = loadForUpdate(id);
        requireDraft(rtv);
        rtv.setStatus(SupplierReturnStatus.CANCELLED);
        rtv.setCancelReason(reason);
        return toResponse(returns.saveAndFlush(rtv));
    }

    // ---------------------------------------------------------------- helpers

    private SupplierReturn load(UUID id) {
        return returns.findById(id).orElseThrow(() -> new NotFoundException("SupplierReturn", id));
    }

    private SupplierReturn loadForUpdate(UUID id) {
        return returns.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("SupplierReturn", id));
    }

    private void requireDraft(SupplierReturn rtv) {
        if (rtv.getStatus() != SupplierReturnStatus.DRAFT) {
            throw new ConflictException("RTV_INVALID_STATE", "The return is " + rtv.getStatus() + ": action not allowed");
        }
    }

    private static String normalizeOrNull(String s) {
        return s == null || s.isBlank() ? null : Codes.normalize(s);
    }

    private void checkLength(String text, int max, String field) {
        if (text != null && text.length() > max) {
            throw new BusinessException("TEXT_TOO_LONG", field + " is longer than " + max + " characters");
        }
    }

    private SupplierReturnResponse toResponse(SupplierReturn rtv) {
        Set<UUID> itemIds = rtv.getLines().stream().map(SupplierReturnLine::getItemId).collect(Collectors.toSet());
        Set<UUID> locationIds = rtv.getLines().stream().map(SupplierReturnLine::getLocationId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) items.findAllByIds(itemIds).forEach(i -> itemMap.put(i.id(), i));
        Map<UUID, LocationResponse> locationMap = new HashMap<>();
        if (!locationIds.isEmpty()) locations.findAllByIds(locationIds).forEach(l -> locationMap.put(l.id(), l));
        return SupplierReturnMapper.toResponse(rtv, suppliers.getById(rtv.getSupplierId()).name(), itemMap, locationMap);
    }
}

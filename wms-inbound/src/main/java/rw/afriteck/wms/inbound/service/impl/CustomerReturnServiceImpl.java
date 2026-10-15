package rw.afriteck.wms.inbound.service.impl;

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
import rw.afriteck.wms.common.security.WorkerLookup;
import rw.afriteck.wms.common.support.PageRules;
import rw.afriteck.wms.foundation.dtos.ItemResponse;
import rw.afriteck.wms.foundation.dtos.LocationResponse;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.foundation.service.ItemService;
import rw.afriteck.wms.foundation.service.LocationService;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.inbound.dtos.*;
import rw.afriteck.wms.inbound.entity.CustomerReturn;
import rw.afriteck.wms.inbound.entity.CustomerReturnLine;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;
import rw.afriteck.wms.inbound.enums.ReturnDisposition;
import rw.afriteck.wms.inbound.mapper.CustomerReturnMapper;
import rw.afriteck.wms.inbound.repository.CustomerReturnLineRepository;
import rw.afriteck.wms.inbound.repository.CustomerReturnRepository;
import rw.afriteck.wms.inbound.service.CustomerReturnService;
import rw.afriteck.wms.inbound.support.InboundNumbers;
import rw.afriteck.wms.inbound.support.InboundReferences;
import rw.afriteck.wms.inbound.support.PutawaySuggester;
import rw.afriteck.wms.inbound.support.TrackingRules;
import rw.afriteck.wms.inventory.dtos.PostStockLine;
import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.service.InventoryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerReturnServiceImpl implements CustomerReturnService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("returnNumber", "status", "createdAt", "receivedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CustomerReturnRepository returns;
    private final CustomerReturnLineRepository returnLines;
    private final WarehouseService warehouses;
    private final CustomerService customers;
    private final LocationService locations;
    private final ItemService items;
    private final InventoryService inventory;
    private final PutawaySuggester suggester;
    private final WorkerLookup workers;
    private final Clock clock;

    /** One validated line of a receive request, before anything is written. */
    private static final class Received {
        CustomerReturnLine line;
        ItemResponse item;
        BigDecimal qty = BigDecimal.ZERO;
        ReturnDisposition disposition;
        UUID requestedLocationId;
        UUID location;
        String lot;
        LocalDate expiry;
        String serial;
    }

    // ================================================================== create

    @Override
    @Transactional
    public CustomerReturnResponse create(CreateCustomerReturnRequest r) {
        if (r == null || r.warehouseId() == null) {
            throw new BusinessException("RETURN_FIELDS_REQUIRED", "A warehouse is required");
        }
        if (r.lines() == null || r.lines().isEmpty()) {
            throw new BusinessException("RETURN_NO_LINES", "A return needs at least one line");
        }
        requireActiveWarehouse(r.warehouseId());
        if (r.customerId() != null) {
            customers.getById(r.customerId());                       // 404 when unknown; inactive customers are fine
        }

        Set<UUID> itemIds = new LinkedHashSet<>();
        for (CustomerReturnLineRequest req : r.lines()) {
            if (req.itemId() == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Every line needs an item");
            }
            itemIds.add(req.itemId());
        }
        Map<UUID, ItemResponse> itemMap = loadActiveItems(itemIds);

        CustomerReturn ret = new CustomerReturn();
        ret.setReturnNumber(InboundNumbers.customerReturn(returns.nextReturnNumber()));
        ret.setWarehouseId(r.warehouseId());
        ret.setCustomerId(r.customerId());
        ret.setSalesOrderId(r.salesOrderId());
        ret.setSalesOrderNumber(Codes.blankToNull(r.salesOrderNumber()));
        ret.setStatus(CustomerReturnStatus.EXPECTED);
        ret.setNotes(Codes.blankToNull(r.notes()));

        Set<String> serialsSeen = new HashSet<>();
        int lineNo = 0;
        for (CustomerReturnLineRequest req : r.lines()) {
            ItemResponse item = itemMap.get(req.itemId());
            BigDecimal qty = toBase(item, req.uom(), req.quantity());
            if (qty.signum() <= 0) {
                throw new BusinessException("QTY_INVALID", "Quantity must be greater than zero (item " + item.sku() + ")");
            }
            String lot = normalizeOrNull(req.lotNumber());
            String serial = normalizeOrNull(req.serialNumber());
            checkExpectedTracking(item, lot, req.expiryDate(), serial, qty);
            if (serial != null && !serialsSeen.add(item.id() + "|" + serial)) {
                throw new BusinessException("SERIAL_DUPLICATED", "Serial " + serial + " appears twice in this return");
            }

            CustomerReturnLine line = new CustomerReturnLine();
            line.setLineNo(++lineNo);
            line.setItemId(item.id());
            line.setExpectedQty(qty);
            line.setLotNumber(lot);
            line.setExpiryDate(req.expiryDate());
            line.setSerialNumber(serial);
            line.setReturnReason(Codes.blankToNull(req.returnReason()));
            ret.addLine(line);
        }
        return toResponse(returns.saveAndFlush(ret));
    }

    /** Creation is lenient (lot, serial, expiry may be unknown until the box is opened) but never contradicts the item. */
    private static void checkExpectedTracking(ItemResponse item, String lot, LocalDate expiry, String serial, BigDecimal qty) {
        switch (item.trackingType()) {
            case NONE -> {
                if (lot != null || serial != null || expiry != null) {
                    throw new BusinessException("TRACKING_MISMATCH",
                            "Item " + item.sku() + " is not lot or serial tracked: no lot, serial or expiry allowed");
                }
            }
            case LOT -> {
                if (serial != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is lot tracked: no serial allowed");
                }
                if (!item.expiryTracked() && expiry != null) {
                    throw new BusinessException("EXPIRY_NOT_ALLOWED", "Item " + item.sku() + " does not track expiry");
                }
            }
            case SERIAL -> {
                if (lot != null || expiry != null) {
                    throw new BusinessException("TRACKING_MISMATCH", "Item " + item.sku() + " is serial tracked: no lot or expiry allowed");
                }
                if (qty.compareTo(BigDecimal.ONE) != 0) {
                    throw new BusinessException("SERIAL_QTY_MUST_BE_ONE",
                            "A serial tracked item needs one line per unit (item " + item.sku() + ")");
                }
            }
        }
    }

    // ================================================================== receive

    @Override
    @Transactional
    public CustomerReturnResponse receive(UUID id, ReceiveCustomerReturnRequest r, UUID actorId) {
        if (actorId == null) {
            throw new BusinessException("ACTOR_REQUIRED", "Receiving needs a user");
        }
        if (r == null || r.lines() == null || r.lines().isEmpty()) {
            throw new BusinessException("RETURN_NO_LINES", "List the lines that were received");
        }
        CustomerReturn ret = returns.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("CustomerReturn", id));
        if (ret.getStatus() != CustomerReturnStatus.EXPECTED) {
            throw new ConflictException("RETURN_INVALID_STATE",
                    "Only an EXPECTED return can be received (current status: " + ret.getStatus() + ")");
        }
        UUID warehouseId = ret.getWarehouseId();
        requireActiveWarehouse(warehouseId);

        Map<UUID, CustomerReturnLine> lineById = ret.getLines().stream()
                .collect(Collectors.toMap(CustomerReturnLine::getId, Function.identity()));
        Map<UUID, ItemResponse> itemMap = items.findAllByIds(
                        ret.getLines().stream().map(CustomerReturnLine::getItemId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));

        // ---- pass 1: validate everything, write nothing
        Map<UUID, Received> byLine = new LinkedHashMap<>();
        Set<String> serialsSeen = new HashSet<>();
        boolean anyReceived = false;

        for (ReceiveReturnLineRequest req : r.lines()) {
            CustomerReturnLine line = req.lineId() == null ? null : lineById.get(req.lineId());
            if (line == null) {
                throw new BusinessException("RETURN_LINE_NOT_FOUND", "Line " + req.lineId() + " is not on this return");
            }
            if (byLine.containsKey(line.getId())) {
                throw new BusinessException("RETURN_LINE_DUPLICATED", "Line " + line.getLineNo() + " is listed twice");
            }
            ItemResponse item = itemMap.get(line.getItemId());
            if (req.receivedQty() == null) {
                throw new BusinessException("QTY_INVALID", "The received quantity is required (use 0 when nothing came back)");
            }

            Received rc = new Received();
            rc.line = line;
            rc.item = item;
            rc.qty = toBase(item, req.uom(), req.receivedQty());
            if (rc.qty.compareTo(line.getExpectedQty()) > 0) {
                throw new BusinessException("RETURN_QTY_EXCEEDS_EXPECTED",
                        "Item " + item.sku() + ": more than the expected " + line.getExpectedQty().stripTrailingZeros().toPlainString()
                                + " was received");
            }

            if (rc.qty.signum() > 0) {
                anyReceived = true;
                if (!item.active()) {
                    throw new BusinessException("ITEM_INACTIVE", "Item " + item.sku() + " is inactive");
                }
                if (req.disposition() == null) {
                    throw new BusinessException("DISPOSITION_REQUIRED", "Choose RESTOCK or DAMAGED for item " + item.sku());
                }
                rc.disposition = req.disposition();
                rc.requestedLocationId = req.locationId();
                // what the worker reads on the label wins over what was expected
                rc.lot = Codes.blankToNull(req.lotNumber()) != null ? Codes.normalize(req.lotNumber()) : line.getLotNumber();
                rc.serial = Codes.blankToNull(req.serialNumber()) != null ? Codes.normalize(req.serialNumber()) : line.getSerialNumber();
                rc.expiry = req.expiryDate() != null ? req.expiryDate() : line.getExpiryDate();
                TrackingRules.check(item, rc.lot, rc.expiry, rc.serial, rc.qty);
                if (rc.serial != null && !serialsSeen.add(item.id() + "|" + rc.serial)) {
                    throw new BusinessException("SERIAL_DUPLICATED", "Serial " + rc.serial + " appears twice in this return");
                }
            }
            byLine.put(line.getId(), rc);
        }
        if (!anyReceived) {
            throw new BusinessException("RETURN_NOTHING_RECEIVED", "Nothing was received: cancel the return instead");
        }
        resolveLocations(warehouseId, byLine.values());

        // ---- pass 2: write and post
        List<PostStockLine> stockLines = new ArrayList<>();
        for (CustomerReturnLine line : ret.getLines()) {
            Received rc = byLine.get(line.getId());
            if (rc == null || rc.qty.signum() == 0) {
                line.setReceivedQty(BigDecimal.ZERO);                 // not listed or nothing came back
                continue;
            }
            line.setReceivedQty(rc.qty);
            line.setDisposition(rc.disposition);
            line.setLocationId(rc.location);
            line.setLotNumber(rc.lot);
            line.setExpiryDate(rc.expiry);
            line.setSerialNumber(rc.serial);
            // unit cost stays null: the original cost is unknown here (valuation treats null as unknown)
            stockLines.add(PostStockLine.receipt(rc.location, rc.item.id(), rc.lot, rc.expiry, rc.serial, rc.qty, null));
        }

        ret.setStatus(CustomerReturnStatus.RECEIVED);
        ret.setReceivedAt(clock.instant());
        ret.setReceivedBy(actorId);
        appendNotes(ret, r.notes());

        inventory.post(new PostStockRequest(
                ret.getId(),                                          // transactionId: the document id
                ret.getReceivedAt(),
                InboundReferences.RMA,
                ret.getId(),
                ret.getReturnNumber(),
                ret.getSalesOrderNumber() == null ? "Customer return" : "Customer return, sales order " + ret.getSalesOrderNumber(),
                stockLines));

        return toResponse(returns.saveAndFlush(ret));
    }

    /** RESTOCK -> STORAGE bin (given or suggested); DAMAGED -> DAMAGED bin (given or the first one). */
    private void resolveLocations(UUID warehouseId, Collection<Received> received) {
        Set<UUID> needSuggestion = new LinkedHashSet<>();
        UUID defaultDamaged = null;

        for (Received rc : received) {
            if (rc.qty.signum() <= 0) {
                continue;
            }
            if (rc.disposition == ReturnDisposition.RESTOCK) {
                if (rc.requestedLocationId != null) {
                    rc.location = checkedBin(rc.requestedLocationId, warehouseId, LocationUsage.STORAGE);
                } else {
                    needSuggestion.add(rc.item.id());
                }
            } else {
                if (rc.requestedLocationId != null) {
                    rc.location = checkedBin(rc.requestedLocationId, warehouseId, LocationUsage.DAMAGED);
                } else {
                    if (defaultDamaged == null) {
                        defaultDamaged = locations.findBinsByUsage(warehouseId, LocationUsage.DAMAGED).stream()   // active bins only
                                .min(Comparator.comparing(LocationResponse::code))
                                .map(LocationResponse::id)
                                .orElseThrow(() -> new BusinessException("NO_DAMAGED_LOCATION",
                                        "The warehouse has no active DAMAGED bin"));
                    }
                    rc.location = defaultDamaged;
                }
            }
        }
        if (!needSuggestion.isEmpty()) {
            Map<UUID, UUID> suggestions = suggester.suggest(warehouseId, needSuggestion);
            for (Received rc : received) {
                if (rc.qty.signum() > 0 && rc.disposition == ReturnDisposition.RESTOCK && rc.location == null) {
                    rc.location = suggestions.get(rc.item.id());
                    if (rc.location == null) {
                        throw new BusinessException("NO_STORAGE_LOCATION", "The warehouse has no active STORAGE bin");
                    }
                }
            }
        }
    }

    private UUID checkedBin(UUID id, UUID warehouseId, LocationUsage usage) {
        LocationResponse loc = locations.requireStorableBin(id);       // BusinessException unless an active BIN
        if (!warehouseId.equals(loc.warehouseId())) {
            throw new BusinessException("LOCATION_WRONG_WAREHOUSE", "Location " + loc.code() + " belongs to another warehouse");
        }
        if (loc.usage() != usage) {
            throw new BusinessException("LOCATION_USAGE_INVALID", "Location " + loc.code() + " must be a " + usage + " bin");
        }
        return loc.id();
    }

    private static void appendNotes(CustomerReturn ret, String extra) {
        String add = Codes.blankToNull(extra);
        if (add == null) {
            return;
        }
        String merged = ret.getNotes() == null ? add : ret.getNotes() + "\n" + add;
        if (merged.length() > 1000) {
            throw new BusinessException("NOTES_TOO_LONG", "The notes are too long (maximum 1000 characters)");
        }
        ret.setNotes(merged);
    }

    // ================================================================== cancel

    @Override
    @Transactional
    public CustomerReturnResponse cancel(UUID id, String reason) {
        String why = Codes.blankToNull(reason);
        if (why == null) {
            throw new BusinessException("CANCEL_REASON_REQUIRED", "A cancellation reason is required");
        }
        CustomerReturn ret = returns.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("CustomerReturn", id));
        if (ret.getStatus() != CustomerReturnStatus.EXPECTED) {
            throw new ConflictException("RETURN_INVALID_STATE",
                    "Only an EXPECTED return can be cancelled (current status: " + ret.getStatus() + ")");
        }
        ret.setStatus(CustomerReturnStatus.CANCELLED);
        ret.setCancelledAt(clock.instant());
        ret.setCancelReason(why);
        return toResponse(returns.saveAndFlush(ret));
    }

    // ================================================================== queries

    @Override
    public CustomerReturnResponse getById(UUID id) {
        CustomerReturn ret = returns.findWithLinesById(id)
                .orElseThrow(() -> new NotFoundException("CustomerReturn", id));
        return toResponse(ret);
    }

    @Override
    public PageResponse<CustomerReturnSummaryResponse> search(UUID warehouseId, Set<CustomerReturnStatus> statuses,
                                                              String q, Pageable pageable) {
        if (warehouseId == null) {
            throw new BusinessException("WAREHOUSE_REQUIRED", "A warehouse is required");
        }
        // an empty IN () list is invalid SQL: no filter means every status
        Set<CustomerReturnStatus> effective = (statuses == null || statuses.isEmpty())
                ? EnumSet.allOf(CustomerReturnStatus.class)
                : EnumSet.copyOf(statuses);
        String pattern = Codes.blankToNull(q) == null ? "%" : Codes.likePattern(q).toLowerCase(Locale.ROOT);
        Pageable safe = PageRules.sanitize(pageable, SORTABLE_FIELDS, DEFAULT_SORT);

        Page<CustomerReturn> page = returns.search(warehouseId, effective, pattern, safe);

        List<UUID> ids = page.getContent().stream().map(CustomerReturn::getId).toList();
        Map<UUID, Integer> lineCounts = ids.isEmpty()
                ? Map.of()
                : returnLines.countByReturn(ids).stream().collect(Collectors.toMap(
                CustomerReturnLineRepository.ReturnCount::getReturnId,
                row -> Math.toIntExact(row.getLineCount())));
        Map<UUID, String> customerNames = customerNames(page.getContent());

        return PageResponse.of(page.map(ret -> CustomerReturnMapper.toSummary(ret,
                ret.getCustomerId() == null ? null : customerNames.get(ret.getCustomerId()),
                lineCounts.getOrDefault(ret.getId(), 0))));
    }

    @Override
    public UUID warehouseIdOf(UUID id) {
        return returns.findById(id)
                .map(CustomerReturn::getWarehouseId)
                .orElseThrow(() -> new NotFoundException("CustomerReturn", id));
    }

    // ================================================================== helpers

    private void requireActiveWarehouse(UUID warehouseId) {
        WarehouseResponse warehouse = warehouses.getById(warehouseId);      // NotFoundException if unknown
        if (!warehouse.active()) {
            throw new BusinessException("WAREHOUSE_INACTIVE", "Warehouse " + warehouse.name() + " is inactive");
        }
    }

    private Map<UUID, ItemResponse> loadActiveItems(Collection<UUID> ids) {
        Map<UUID, ItemResponse> found = items.findAllByIds(ids).stream()
                .collect(Collectors.toMap(ItemResponse::id, Function.identity()));
        for (UUID id : ids) {
            ItemResponse item = found.get(id);
            if (item == null) {
                throw new BusinessException("ITEM_NOT_FOUND", "Item " + id + " does not exist");
            }
            if (!item.active()) {
                throw new BusinessException("ITEM_INACTIVE", "Item " + item.sku() + " is inactive");
            }
        }
        return found;
    }

    /** Quantity in `uom` to the item's base UOM; null or zero is ZERO. */
    private BigDecimal toBase(ItemResponse item, String uom, BigDecimal qty) {
        if (qty == null || qty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (qty.signum() < 0) {
            throw new BusinessException("QTY_INVALID", "Quantities cannot be negative (item " + item.sku() + ")");
        }
        String u = Codes.blankToNull(uom);
        BigDecimal base = (u == null || u.equalsIgnoreCase(item.baseUom()))
                ? qty
                : items.toBaseQuantity(item.id(), Codes.normalize(u), qty);
        return base.setScale(4, RoundingMode.HALF_UP);
    }

    /** Lot and serial numbers are normalized exactly as operations and inventory store them. */
    private static String normalizeOrNull(String raw) {
        String v = Codes.blankToNull(raw);
        return v == null ? null : Codes.normalize(v);
    }

    private CustomerReturnResponse toResponse(CustomerReturn ret) {
        Set<UUID> itemIds = ret.getLines().stream().map(CustomerReturnLine::getItemId).collect(Collectors.toSet());
        Map<UUID, ItemResponse> itemMap = itemIds.isEmpty()
                ? Map.of()
                : items.findAllByIds(itemIds).stream().collect(Collectors.toMap(ItemResponse::id, Function.identity()));

        Set<UUID> locationIds = ret.getLines().stream()
                .map(CustomerReturnLine::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> locationCodes = locationIds.isEmpty()
                ? Map.of()
                : locations.findAllByIds(locationIds).stream()
                .collect(Collectors.toMap(LocationResponse::id, LocationResponse::code));

        String customerName = ret.getCustomerId() == null ? null : customers.getById(ret.getCustomerId()).name();
        Map<UUID, String> userNames = ret.getReceivedBy() == null
                ? Map.of()
                : workers.findDisplayNames(List.of(ret.getReceivedBy()));
        return CustomerReturnMapper.toResponse(ret, customerName, itemMap, locationCodes, userNames);
    }

    /** CustomerService has no batch method: one call per DISTINCT customer on the page. */
    private Map<UUID, String> customerNames(List<CustomerReturn> content) {
        Map<UUID, String> names = new HashMap<>();
        for (UUID id : content.stream().map(CustomerReturn::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet())) {
            names.put(id, customers.getById(id).name());
        }
        return names;
    }
}
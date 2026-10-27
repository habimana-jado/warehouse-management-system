package rw.afriteck.wms.web.outbound;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.enums.LocationUsage;
import rw.afriteck.wms.inventory.dtos.StockLevelResponse;
import rw.afriteck.wms.inventory.service.InventoryQueryService;
import rw.afriteck.wms.operations.dtos.TaskResponse;
import rw.afriteck.wms.operations.enums.TaskStatus;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.outbound.dtos.PickLineResponse;
import rw.afriteck.wms.outbound.dtos.PickListResponse;
import rw.afriteck.wms.outbound.enums.PickLineStatus;
import rw.afriteck.wms.outbound.enums.PickListStatus;
import rw.afriteck.wms.outbound.service.PickListService;
import rw.afriteck.wms.web.inventory.BinOptions;

import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
class PickListViews {

    private static final int STOCK_ROWS = 12;

    private final PickListService pickLists;
    private final TaskService taskService;
    private final InventoryQueryService inventoryQuery;
    private final BinOptions binOptions;
    private final UserContext userContext;

    void fill(Model model, UUID id) {
        fill(model, id, Map.of(), Map.of());
    }

    /** forms and errors replace the defaults of single lines (a failed confirm keeps what the picker typed). */
    void fill(Model model, UUID id, Map<UUID, PickConfirmForm> forms, Map<UUID, String> errors) {
        userContext.requireWarehouseAccess(pickLists.warehouseIdOf(id));
        PickListResponse p = pickLists.getById(id);
        TaskResponse task = p.taskId() == null ? null : taskService.getById(p.taskId());

        boolean privileged = privileged();
        UUID me = userContext.currentUserId();
        boolean assignee = task != null && me.equals(task.assigneeId());
        // a picker reads only the list of their own task (same rule as the REST controller); managers and viewers read all
        if (!privileged && !userContext.hasRole("VIEWER") && !assignee) {
            throw new ForbiddenException("This pick list is assigned to another user");
        }

        boolean open = PickListStatus.OPEN.contains(p.status());
        boolean started = task != null
                && (task.status() == TaskStatus.ASSIGNED || task.status() == TaskStatus.IN_PROGRESS);
        boolean mayPick = privileged || userContext.hasRole("PICKER");
        boolean canWork = open && started && mayPick && (privileged || assignee);

        List<PickLineView> lines = new ArrayList<>();
        for (PickLineResponse l : p.lines()) {
            boolean work = canWork && l.status() == PickLineStatus.PENDING;
            String tracking = PickConfirmForm.trackingName(l);
            PickConfirmForm form = null;
            List<StockOption> stock = List.of();
            int more = 0;
            if (work) {
                form = forms.getOrDefault(l.id(), PickConfirmForm.suggested(l));
                List<StockOption> all = stockOptions(p.warehouseId(), l.itemId());
                stock = all.stream().limit(STOCK_ROWS).toList();
                more = Math.max(0, all.size() - STOCK_ROWS);
            }
            boolean quick = work && !"SERIAL".equals(tracking) && l.suggestedLocationId() != null;
            lines.add(new PickLineView(l, tracking, work, quick, form, stock, more, errors.get(l.id())));
        }

        List<BinOptions.BinOption> storageBins = binOptions.forWarehouse(p.warehouseId()).stream()
                .filter(b -> b.usage() == LocationUsage.STORAGE).toList();
        model.addAttribute("view", new PickListView(p, p.taskId(), task == null ? null : task.taskNumber(), open,
                open && !started && mayPick, privileged && open, privileged, lines, storageBins));
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelPickListForm());
        }
    }

    /** The line view for the add/remove row fragments. */
    PickLineView line(Model model, UUID lineId) {
        PickListView v = (PickListView) model.getAttribute("view");
        return v.lines().stream().filter(l -> l.line().id().equals(lineId)).findFirst()
                .orElseThrow(() -> new NotFoundException("PickLine", lineId));
    }

    /** Tracking of one line, needed to read the serial text of the posted form. */
    boolean isSerial(UUID pickListId, UUID lineId) {
        return pickLists.getById(pickListId).lines().stream()
                .filter(l -> l.id().equals(lineId)).findFirst()
                .map(l -> "SERIAL".equals(PickConfirmForm.trackingName(l)))
                .orElseThrow(() -> new NotFoundException("PickLine", lineId));
    }

    boolean privileged() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private List<StockOption> stockOptions(UUID warehouseId, UUID itemId) {
        return inventoryQuery.findAvailable(warehouseId, itemId).stream()
                .filter(s -> s.locationUsage() == LocationUsage.STORAGE)
                .sorted(Comparator.comparing(StockLevelResponse::expiryDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                        .thenComparing(StockLevelResponse::locationCode))
                .map(s -> new StockOption(s.locationCode(), s.lotNumber(), s.expiryDate(), s.serialNumber(), s.quantity()))
                .toList();
    }
}

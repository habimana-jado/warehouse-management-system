package rw.afriteck.wms.web.inventory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import rw.afriteck.wms.common.dtos.PageResponse;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.dtos.WarehouseResponse;
import rw.afriteck.wms.foundation.service.WarehouseService;
import rw.afriteck.wms.inventory.dtos.*;
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;
import rw.afriteck.wms.inventory.service.TransferService;
import rw.afriteck.wms.inventory.support.InventoryReferences;
import rw.afriteck.wms.operations.enums.TaskPriority;
import rw.afriteck.wms.operations.service.TaskService;
import rw.afriteck.wms.web.config.WebAccess;
import rw.afriteck.wms.web.support.*;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
class TransferPageController {

    private static final String PANEL = "inventory/transfer-detail :: panel";
    private static final String PAGE = "inventory/transfer-detail";

    /** Service error code -> form field; every other code becomes a global error on the form. */
    private static final Map<String, String> ERROR_FIELDS = Map.of(
            "TRANSFER_SAME_WAREHOUSE", "destinationWarehouseId",
            "WAREHOUSE_REQUIRED", "destinationWarehouseId",
            "WAREHOUSE_INACTIVE", "destinationWarehouseId");

    private enum Access { SOURCE, DESTINATION, EITHER }

    private final TransferService transfers;
    private final TaskService taskService;
    private final WarehouseService warehouseService;
    private final WarehouseContext warehouseContext;
    private final UserContext userContext;
    private final BinOptions binOptions;
    private final ItemResolver itemResolver;
    private final UiLinks uiLinks;

    @Value("${wms.ui.zone:Africa/Kigali}")
    private String zone;

    @ModelAttribute("pageSizes")
    List<Integer> pageSizes() {
        return InventoryPages.PAGE_SIZES;
    }

    @ModelAttribute("statuses")
    List<TransferStatus> statuses() {
        return List.of(TransferStatus.values());
    }

    @ModelAttribute("priorities")
    List<TaskPriority> priorities() {
        return List.of(TaskPriority.values());
    }

    // ================================================================== lists

    @GetMapping("/transfers")
    @PreAuthorize(WebAccess.BOARD)
    String board(@ModelAttribute("filter") TransferFilterForm filter, BindingResult binding,
                 @PageableDefault(size = 15) Pageable pageable, Model model, HttpServletRequest request) {
        UUID warehouseId = warehouseContext.require();
        Set<TransferStatus> statuses = filter.getStatus() == null ? Set.of() : EnumSet.of(filter.getStatus());
        PageResponse<TransferSummaryResponse> result =
                transfers.search(new TransferFilter(warehouseId, statuses, filter.getQ()), pageable);
        Map<UUID, String> names = warehouseNames();
        model.addAttribute("page", PageView.of(result, s -> row(s, names)));
        InventoryPages.sortLinks(model, uiLinks, "createdAt", "transferNumber", "status");
        return Htmx.isFragmentRequest(request) ? "inventory/transfers :: results" : "inventory/transfers";
    }

    /** Goods on their way to the selected warehouse (a UI list for receivers; see decision 5). */
    @GetMapping("/transfers/incoming")
    @PreAuthorize(WebAccess.WORKER)
    String incoming(Model model) {
        UUID warehouseId = warehouseContext.require();
        PageResponse<TransferSummaryResponse> result = transfers.search(
                new TransferFilter(warehouseId, EnumSet.of(TransferStatus.IN_TRANSIT), null),
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, String> names = warehouseNames();
        model.addAttribute("rows", result.content().stream()
                .filter(s -> warehouseId.equals(s.destinationWarehouseId()))
                .map(s -> row(s, names)).toList());
        return "inventory/transfers-incoming";
    }

    private TransferRow row(TransferSummaryResponse s, Map<UUID, String> names) {
        return new TransferRow(s, names.getOrDefault(s.sourceWarehouseId(), "-"), names.getOrDefault(s.destinationWarehouseId(), "-"));
    }

    private Map<UUID, String> warehouseNames() {
        return warehouseService.findAllActive().stream()
                .collect(Collectors.toMap(WarehouseResponse::id, WarehouseResponse::name));
    }

    // ================================================================== create

    @GetMapping("/transfers/new")
    @PreAuthorize(WebAccess.WORKER)
    String newForm(Model model) {
        TransferForm form = new TransferForm();
        form.getLines().add(new TransferLineForm());
        model.addAttribute("form", form);
        fillForm(form, model);
        return "inventory/transfer-form";
    }

    @PostMapping("/transfers")
    @PreAuthorize(WebAccess.WORKER)
    String create(@Valid @ModelAttribute("form") TransferForm form, BindingResult result,
                  Model model, RedirectAttributes redirect) {
        UUID warehouseId = warehouseContext.require();
        if (!form.isMoveNow() && !isManager()) {
            throw new ForbiddenException("Only managers can plan or send transfers");
        }
        List<UUID> itemIds = InventoryPages.resolveItems(itemResolver,
                form.getLines().stream().map(TransferLineForm::getItem).toList(), result);
        validateMode(form, result);
        if (!result.hasErrors()) {
            try {
                TransferResponse created = transfers.create(form.toRequest(warehouseId, itemIds, ZoneId.of(zone)));
                String key = created.status() == TransferStatus.COMPLETED
                        ? "inventory.transfer.flash.moved" : "inventory.transfer.flash.created";
                redirect.addFlashAttribute("flash", Flash.success(key, created.transferNumber()));
                return "redirect:/transfers/" + created.id();
            } catch (BusinessException e) {
                FormErrors.apply(result, e, ERROR_FIELDS);
            }
        }
        fillForm(form, model);
        return "inventory/transfer-form";
    }

    /** htmx: the mode changed, so the fields that depend on it are drawn again. */
    @PostMapping("/transfers/new/refresh")
    @PreAuthorize(WebAccess.WORKER)
    String refresh(@ModelAttribute("form") TransferForm form, BindingResult ignored, Model model) {
        fillForm(form, model);
        return "inventory/transfer-form :: fields";
    }

    @PostMapping("/transfers/new/lines/add")
    @PreAuthorize(WebAccess.WORKER)
    String addLine(@ModelAttribute("form") TransferForm form, BindingResult ignored, Model model) {
        form.getLines().add(new TransferLineForm());
        fillForm(form, model);
        return "inventory/transfer-form :: linesCard";
    }

    @PostMapping("/transfers/new/lines/remove")
    @PreAuthorize(WebAccess.WORKER)
    String removeLine(@ModelAttribute("form") TransferForm form, BindingResult ignored,
                      @RequestParam int index, Model model) {
        if (form.getLines().size() > 1 && index >= 0 && index < form.getLines().size()) {
            form.getLines().remove(index);
        }
        fillForm(form, model);
        return "inventory/transfer-form :: linesCard";
    }

    private void fillForm(TransferForm form, Model model) {
        UUID warehouseId = warehouseContext.require();
        boolean manager = isManager();
        if (!manager) {
            form.setMode(TransferMode.MOVE_NOW);                      // workers can only move stock now
        }
        model.addAttribute("bins", binOptions.forWarehouse(warehouseId));
        model.addAttribute("warehouses", warehouseService.findAllActive().stream()
                .filter(w -> !w.id().equals(warehouseId)).toList());
        model.addAttribute("modes", manager ? List.of(TransferMode.values()) : List.of(TransferMode.MOVE_NOW));
        model.addAttribute("menu", "new-transfer");
    }

    /** Checks that depend on the mode (the service checks them too; this puts them on the right field). */
    private void validateMode(TransferForm form, BindingResult result) {
        if (form.isInter()) {
            if (form.getDestinationWarehouseId() == null) {
                result.rejectValue("destinationWarehouseId", "inventory.transfer.destinationRequired", "Choose the destination warehouse");
            }
            return;
        }
        for (int i = 0; i < form.getLines().size(); i++) {
            if (form.getLines().get(i).getToLocationId() == null) {
                result.rejectValue("lines[" + i + "].toLocationId", "inventory.transfer.toRequired", "Choose the destination bin");
            }
        }
    }

    // ================================================================== detail and actions

    @GetMapping("/transfers/{id}")
    @PreAuthorize(WebAccess.ANY_USER)
    String detail(@PathVariable UUID id, Model model) {
        requireAccess(id, Access.EITHER);
        fillPanel(model, transfers.getById(id));
        return PAGE;
    }

    @PostMapping("/transfers/{id}/lines/{lineId}/confirm")
    @PreAuthorize(WebAccess.WORKER)
    String confirm(@PathVariable UUID id, @PathVariable UUID lineId,
                   @RequestParam(required = false) BigDecimal quantity,
                   @RequestParam(required = false) String shortReason,
                   HttpServletRequest request, Model model, RedirectAttributes redirect) {
        return run(id, Access.SOURCE, "inventory.transfer.flash.confirmed",
                () -> transfers.confirmLine(id, lineId, new ConfirmTransferLineRequest(quantity, shortReason),
                        userContext.currentUserId(), isManager()),
                request, model, redirect);
    }

    @PostMapping("/transfers/{id}/ship")
    @PreAuthorize(WebAccess.MANAGER)
    String ship(@PathVariable UUID id, HttpServletRequest request, Model model, RedirectAttributes redirect) {
        return run(id, Access.SOURCE, "inventory.transfer.flash.shipped", () -> transfers.ship(id), request, model, redirect);
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize(WebAccess.WORKER)
    String receive(@PathVariable UUID id, @Valid @ModelAttribute("receiveForm") ReceiveForm form, BindingResult result,
                   HttpServletRequest request, Model model, RedirectAttributes redirect) {
        requireAccess(id, Access.DESTINATION);
        for (int i = 0; i < form.getLines().size(); i++) {
            ReceiveLineForm l = form.getLines().get(i);
            if (l.getQuantity() != null && l.getToLocationId() == null) {
                result.rejectValue("lines[" + i + "].toLocationId", "inventory.transfer.toRequired", "Choose the destination bin");
            }
        }
        if (result.hasErrors()) {                                       // re-render with the field errors, status 200
            fillPanel(model, transfers.getById(id));
            return Htmx.isFragmentRequest(request) ? PANEL : PAGE;
        }
        return run(id, Access.DESTINATION, "inventory.transfer.flash.received",
                () -> transfers.receive(id, form.toRequest()), request, model, redirect);
    }

    @PostMapping("/transfers/{id}/cancel")
    @PreAuthorize(WebAccess.MANAGER)
    String cancel(@PathVariable UUID id, @Valid @ModelAttribute("cancelForm") CancelTransferForm form, BindingResult result,
                  HttpServletRequest request, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            requireAccess(id, Access.EITHER);
            fillPanel(model, transfers.getById(id));
            return Htmx.isFragmentRequest(request) ? PANEL : PAGE;
        }
        return run(id, Access.EITHER, "inventory.transfer.flash.cancelled",
                () -> transfers.cancel(id, form.getReason()), request, model, redirect);
    }

    /** Runs one action: htmx gets the re-rendered panel, a plain post is redirected (post / redirect / get). */
    private String run(UUID id, Access access, String messageKey, Supplier<TransferResponse> action,
                       HttpServletRequest request, Model model, RedirectAttributes redirect) {
        requireAccess(id, access);
        boolean htmx = Htmx.isFragmentRequest(request);
        try {
            TransferResponse updated = action.get();
            if (htmx) {
                fillPanel(model, updated);
                model.addAttribute("notice", messageKey);
                return PANEL;
            }
            redirect.addFlashAttribute("flash", Flash.success(messageKey, updated.transferNumber()));
            return "redirect:/transfers/" + id;
        } catch (BusinessException e) {   // e.g. INSUFFICIENT_STOCK, TRANSFER_TASK_NOT_STARTED, task assigned to someone else
            if (htmx) {
                fillPanel(model, transfers.getById(id));
                model.addAttribute("error", e.getMessage());
                return PANEL;
            }
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfers/" + id;
        }
    }

    // ================================================================== helpers

    private void fillPanel(Model model, TransferResponse t) {
        boolean pending = t.status() == TransferStatus.PENDING;
        boolean inTransit = t.status() == TransferStatus.IN_TRANSIT;
        boolean inter = t.type() == TransferType.INTER_WAREHOUSE;
        boolean manager = isManager();

        List<TransferLineRow> rows = t.lines().stream()
                .map(l -> new TransferLineRow(l,
                        pending && l.shortReason() == null && l.shippedQty().compareTo(l.quantity()) < 0))
                .toList();
        boolean anyPending = rows.stream().anyMatch(TransferLineRow::pending);
        boolean partiallyReceived = inTransit && t.lines().stream().anyMatch(l -> l.receivedQty().signum() > 0);
        boolean canReceive = inTransit && inter && isWorker() && userContext.canAccessWarehouse(t.destinationWarehouseId());
        List<TransferLineRow> receiveLines = canReceive
                ? rows.stream().filter(r -> r.line().inTransitQty().signum() > 0).toList() : List.of();

        model.addAttribute("view", new TransferView(t,
                warehouseService.getById(t.sourceWarehouseId()).name(),
                warehouseService.getById(t.destinationWarehouseId()).name(),
                rows, receiveLines,
                taskService.findByReference(InventoryReferences.STOCK_TRANSFER, t.id()),
                anyPending && isWorker(),
                pending && inter && manager && anyPending,
                canReceive,
                manager && (pending || (inTransit && !partiallyReceived)),
                partiallyReceived));
        if (canReceive) {
            model.addAttribute("destBins", binOptions.forWarehouse(t.destinationWarehouseId()));
        }
        if (!model.containsAttribute("receiveForm")) {                  // keep the bound form when it carries errors
            model.addAttribute("receiveForm", ReceiveForm.from(receiveLines));
        }
        if (!model.containsAttribute("cancelForm")) {
            model.addAttribute("cancelForm", new CancelTransferForm());
        }
        model.addAttribute("menu", isBoard() ? "transfers" : "incoming-transfers");
    }

    private void requireAccess(UUID id, Access access) {
        TransferAccessInfo info = transfers.accessInfo(id);
        switch (access) {
            case SOURCE -> userContext.requireWarehouseAccess(info.sourceWarehouseId());
            case DESTINATION -> userContext.requireWarehouseAccess(info.destinationWarehouseId());
            case EITHER -> {
                if (!userContext.canAccessWarehouse(info.sourceWarehouseId())
                        && !userContext.canAccessWarehouse(info.destinationWarehouseId())) {
                    throw new ForbiddenException("No access to this transfer");
                }
            }
        }
    }

    private boolean isManager() {
        return userContext.isAdmin() || userContext.hasRole("WAREHOUSE_MANAGER");
    }

    private boolean isBoard() {
        return isManager() || userContext.hasRole("VIEWER");
    }

    private boolean isWorker() {
        return isManager() || userContext.hasRole("RECEIVER") || userContext.hasRole("PICKER");
    }
}

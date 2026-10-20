package rw.afriteck.wms.web.support;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rw.afriteck.wms.common.exception.BusinessException;
import rw.afriteck.wms.common.exception.ForbiddenException;
import rw.afriteck.wms.common.security.UserContext;
import rw.afriteck.wms.foundation.service.WarehouseService;

import java.util.List;
import java.util.UUID;

@Component("warehouseContext")
@RequiredArgsConstructor
public class WarehouseContext {

    private static final String KEY = "wms.warehouseId";

    private final WarehouseService warehouses;
    private final UserContext userContext;
    private final HttpSession session;          // Spring injects a request-aware proxy

    /** The warehouses this user may use, and the effective one (stored choice, else the first). One query. */
    public WarehouseSelection selection() {
        List<WarehouseOption> options = warehouses.findAllActive().stream()
                .filter(w -> userContext.canAccessWarehouse(w.id()))
                .map(w -> new WarehouseOption(w.id(), w.code(), w.name()))
                .toList();
        UUID stored = (UUID) session.getAttribute(KEY);
        WarehouseOption current = options.stream().filter(o -> o.id().equals(stored)).findFirst()
                .orElse(options.isEmpty() ? null : options.get(0));
        return new WarehouseSelection(current, options);
    }

    public WarehouseOption requireCurrent() {
        WarehouseOption current = selection().current();
        if (current == null) {
            throw new BusinessException("NO_WAREHOUSE", "No warehouse is assigned to your account");
        }
        return current;
    }

    public UUID require() { return requireCurrent().id(); }

    public void select(UUID warehouseId) {
        boolean allowed = selection().options().stream().anyMatch(o -> o.id().equals(warehouseId));
        if (!allowed) throw new ForbiddenException("No access to warehouse " + warehouseId);
        session.setAttribute(KEY, warehouseId);
    }
}

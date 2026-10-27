package rw.afriteck.wms.web.operations;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class TaskReferenceLinks {

    /**
     * referenceType -> UI path, {id} is replaced by the reference id. EMPTY ON PURPOSE: each owning module's UI chat adds its
     * line once its detail screen exists (otherwise the link would 404). The last entry has no trailing comma.
     *   "PICK_LIST",      "/pick-lists/{id}"       (outbound: pick confirmation)
     */
    private static final Map<String, String> ROUTES = Map.of(
            "STOCK_TRANSFER", "/transfers/{id}",
            "STOCK_COUNT", "/counts/{id}",
            "GOODS_RECEIPT", "/goods-receipts/{id}"
    );

    /** UI path of the document behind a task, or null when that module has no screen yet. */
    public String urlFor(String referenceType, UUID referenceId) {
        String route = referenceType == null ? null : ROUTES.get(referenceType);
        return route == null || referenceId == null ? null : route.replace("{id}", referenceId.toString());
    }
}

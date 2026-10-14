package rw.afriteck.wms.inventory.service;

import rw.afriteck.wms.inventory.dtos.PostStockRequest;
import rw.afriteck.wms.inventory.dtos.PostStockResponse;

public interface InventoryService {

    /**
     * The only way stock changes. Joins the caller's transaction: post stock FIRST, then call
     * TaskService.completeLine. Any failure rolls back every line of the request.
     */
    PostStockResponse post(PostStockRequest request);

    /**
     * Dry run of post: same item, location, tracking, lot/expiry and stock-sufficiency rules, but NO locks and NO writes.
     * Advisory only: stock can change before the real post. Throws the same BusinessException codes as post.
     * Not checked here: SERIAL_ALREADY_IN_STOCK (post catches it).
     */
    void check(PostStockRequest request);
}

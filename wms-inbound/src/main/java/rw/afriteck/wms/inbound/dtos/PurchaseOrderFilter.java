package rw.afriteck.wms.inbound.dtos;

import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** warehouseId is mandatory (controllers check access on it). Both dates are INCLUSIVE (they are calendar dates). */
public record PurchaseOrderFilter(UUID warehouseId,
                                  Set<PurchaseOrderStatus> statuses,
                                  UUID supplierId,
                                  UUID itemId,
                                  LocalDate orderDateFrom,
                                  LocalDate orderDateTo,
                                  String q) {}
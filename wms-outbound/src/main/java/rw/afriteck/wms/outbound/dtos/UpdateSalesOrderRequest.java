package rw.afriteck.wms.outbound.dtos;

import java.time.LocalDate;
import java.util.List;

/** DRAFT only. `lines` is the COMPLETE list, synced by diff and matched on itemId. Customer and warehouse cannot change. */
public record UpdateSalesOrderRequest(LocalDate orderDate, LocalDate requiredDate, String currency,
                                      String customerReference, String notes, List<SalesOrderLineRequest> lines) {}

package rw.afriteck.wms.outbound.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateSalesOrderRequest(UUID customerId, UUID warehouseId, LocalDate orderDate, LocalDate requiredDate,
                                      String currency, String customerReference, String notes,
                                      List<SalesOrderLineRequest> lines) {}

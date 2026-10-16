package rw.afriteck.wms.outbound.dtos;

import rw.afriteck.wms.outbound.enums.SalesOrderStatus;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record SalesOrderFilter(UUID warehouseId, Set<SalesOrderStatus> statuses, UUID customerId, UUID itemId,
                               LocalDate orderDateFrom, LocalDate orderDateTo, String q) {}   // both dates inclusive

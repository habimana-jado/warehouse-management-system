package rw.afriteck.wms.outbound.dtos;

import java.util.List;

public record ConfirmSalesOrderResponse(SalesOrderResponse order, List<AvailabilityWarning> warnings) {}


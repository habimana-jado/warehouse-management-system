package rw.afriteck.wms.reporting.dtos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShipmentsReportResponse(UUID warehouseId, LocalDate from, LocalDate toExclusive,
                                      long totalShipments, long totalPackages, long totalDelivered, List<ShipmentDayResponse> days) {}

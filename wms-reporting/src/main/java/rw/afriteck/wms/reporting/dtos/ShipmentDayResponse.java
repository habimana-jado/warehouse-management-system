package rw.afriteck.wms.reporting.dtos;

import java.time.LocalDate;

public record ShipmentDayResponse(LocalDate day, long shipments, long packages, long delivered) {}

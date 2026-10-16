package rw.afriteck.wms.outbound.dtos;

import java.util.List;

/** `lines` null or empty = ship everything that is picked and not yet shipped. */
public record CreateShipmentRequest(String shipToAddress, String carrier, String trackingReference,
                                    Integer packageCount, String notes, List<ShipmentLineRequest> lines) {}

package rw.afriteck.wms.foundation.dtos;

import java.util.UUID;

public record WarehouseResponse(
        UUID id, String code, String name,
        String addressLine, String city, String country,
        boolean active) {
}

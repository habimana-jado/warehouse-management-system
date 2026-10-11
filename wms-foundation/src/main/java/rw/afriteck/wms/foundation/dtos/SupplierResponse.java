package rw.afriteck.wms.foundation.dtos;

import java.util.UUID;

public record SupplierResponse(
        UUID id, String code, String name, String contactPerson,
        String phone, String email, String addressLine,
        String city, String country, boolean active) {
}

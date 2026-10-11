package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 120) String name,
        String addressLine, String city, String country) {
}

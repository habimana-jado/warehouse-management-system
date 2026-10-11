package rw.afriteck.wms.foundation.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 150) String name,
        String contactPerson, String phone, @Email String email,
        String addressLine, String city, String country) {
}

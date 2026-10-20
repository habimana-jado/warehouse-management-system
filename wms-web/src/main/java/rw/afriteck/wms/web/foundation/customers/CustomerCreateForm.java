package rw.afriteck.wms.web.foundation.customers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.CreateCustomerRequest;
import rw.afriteck.wms.foundation.dtos.CreateSupplierRequest;

@Getter
@Setter
public class CustomerCreateForm {
    @NotBlank
    @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9._-]+")
    private String code;
    @NotBlank @Size(max = 150)
    private String name;
    @Size(max = 150)
    private String contactPerson;
    @Size(max = 50)
    private String phone;
    @Email @Size(max = 255)
    private String email;
    @Size(max = 255)
    private String addressLine;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String country;

    public CreateCustomerRequest toRequest() {
        return new CreateCustomerRequest(code, name, Codes.blankToNull(contactPerson), Codes.blankToNull(phone),
                Codes.blankToNull(email), Codes.blankToNull(addressLine), Codes.blankToNull(city),
                Codes.blankToNull(country));
    }
}


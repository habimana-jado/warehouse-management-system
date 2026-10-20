package rw.afriteck.wms.web.foundation.customers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.support.Codes;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.UpdateCustomerRequest;
import rw.afriteck.wms.foundation.dtos.UpdateSupplierRequest;

@Getter @Setter
public class CustomerEditForm {
    @NotBlank @Size(max = 150)
    private String name;
    @Size(max = 150)
    private String contactPerson;
    @Size(max = 50)
    private String phone;
    @Email
    @Size(max = 255)
    private String email;
    @Size(max = 255)
    private String addressLine;
    @Size(max = 100)
    private String city;
    @Size(max = 100)
    private String country;

    public static CustomerEditForm from(CustomerResponse s) {
        CustomerEditForm f = new CustomerEditForm();
        f.name = s.name();
        f.contactPerson = s.contactPerson();
        f.phone = s.phone();
        f.email = s.email();
        f.addressLine = s.addressLine();
        f.city = s.city();
        f.country = s.country();
        return f;
    }

    public UpdateCustomerRequest toRequest() {
        return new UpdateCustomerRequest(name, Codes.blankToNull(contactPerson), Codes.blankToNull(phone),
                Codes.blankToNull(email), Codes.blankToNull(addressLine), Codes.blankToNull(city),
                Codes.blankToNull(country));
    }
}

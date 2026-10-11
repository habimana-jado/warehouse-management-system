package rw.afriteck.wms.foundation.mapper;

import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.entity.Customer;
import rw.afriteck.wms.foundation.entity.Supplier;

public final class PartnerMapper {
    private PartnerMapper() {}

    public static SupplierResponse toSupplier(Supplier s) {
        return new SupplierResponse(s.getId(), s.getCode(), s.getName(), s.getContactPerson(),
                s.getPhone(), s.getEmail(), s.getAddressLine(), s.getCity(), s.getCountry(), s.isActive());
    }

    public static CustomerResponse toCustomer(Customer s) {
        return new CustomerResponse(s.getId(), s.getCode(), s.getName(), s.getContactPerson(),
                s.getPhone(), s.getEmail(), s.getAddressLine(), s.getCity(), s.getCountry(), s.isActive());
    }
}

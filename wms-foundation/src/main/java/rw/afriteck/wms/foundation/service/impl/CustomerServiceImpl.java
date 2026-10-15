package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.CreateCustomerRequest;
import rw.afriteck.wms.foundation.dtos.CustomerResponse;
import rw.afriteck.wms.foundation.dtos.UpdateCustomerRequest;
import rw.afriteck.wms.foundation.entity.Customer;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.mapper.PartnerMapper;
import rw.afriteck.wms.foundation.repository.CustomerRepository;
import rw.afriteck.wms.foundation.service.CustomerService;
import rw.afriteck.wms.common.support.Codes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customers;
    private final DeactivationGuards guards;

    @Override
    public CustomerResponse create(CreateCustomerRequest r) {
        //TODO: Generate this customer code by the system not by user
        String code = Codes.normalize(r.code());
        if (customers.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("CUSTOMER_CODE_EXISTS", "Customer code already exists: " + code);
        }
        Customer s = new Customer();
        s.setCode(code);
        apply(s, r.name(), r.contactPerson(), r.phone(), r.email(), r.addressLine(), r.city(), r.country());
        return PartnerMapper.toCustomer(customers.save(s));
    }

    @Override
    public CustomerResponse update(UUID id, UpdateCustomerRequest r) {
        Customer s = find(id);
        apply(s, r.name(), r.contactPerson(), r.phone(), r.email(), r.addressLine(), r.city(), r.country());
        return PartnerMapper.toCustomer(s);
    }

    @Override
    public CustomerResponse activate(UUID id) {
        Customer s = find(id);
        s.setActive(true);
        return PartnerMapper.toCustomer(s);
    }

    @Override
    public CustomerResponse deactivate(UUID id) {
        Customer s = find(id);
        guards.check(DeactivationGuard.Target.CUSTOMER, id);       // e.g. inbound: open purchase orders
        s.setActive(false);
        return PartnerMapper.toCustomer(s);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CustomerResponse> search(String q, Pageable pageable) {
        return customers.search(Codes.likePattern(q), pageable)
                .map(PartnerMapper::toCustomer);
    }

    @Override @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return PartnerMapper.toCustomer(find(id));
    }

    private Customer find(UUID id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer", id));
    }

    private void apply(Customer s, String name, String contact, String phone, String email,
                       String address, String city, String country) {
        s.setName(name.trim());
        s.setContactPerson(Codes.blankToNull(contact));
        s.setPhone(Codes.blankToNull(phone));
        s.setEmail(Codes.blankToNull(email));
        s.setAddressLine(Codes.blankToNull(address));
        s.setCity(Codes.blankToNull(city));
        s.setCountry(Codes.blankToNull(country));
    }
}

package rw.afriteck.wms.foundation.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.common.exception.ConflictException;
import rw.afriteck.wms.common.exception.NotFoundException;
import rw.afriteck.wms.foundation.dtos.CreateSupplierRequest;
import rw.afriteck.wms.foundation.dtos.SupplierResponse;
import rw.afriteck.wms.foundation.dtos.UpdateSupplierRequest;
import rw.afriteck.wms.foundation.entity.Supplier;
import rw.afriteck.wms.foundation.guard.DeactivationGuard;
import rw.afriteck.wms.foundation.guard.DeactivationGuards;
import rw.afriteck.wms.foundation.mapper.PartnerMapper;
import rw.afriteck.wms.foundation.repository.SupplierRepository;
import rw.afriteck.wms.foundation.service.SupplierService;
import rw.afriteck.wms.common.support.Codes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository suppliers;
    private final DeactivationGuards guards;

    @Override
    public SupplierResponse create(CreateSupplierRequest r) {
        //TODO: Generate this supplier code by the system not by user
        String code = Codes.normalize(r.code());
        if (suppliers.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("SUPPLIER_CODE_EXISTS", "Supplier code already exists: " + code);
        }
        Supplier s = new Supplier();
        s.setCode(code);
        apply(s, r.name(), r.contactPerson(), r.phone(), r.email(), r.addressLine(), r.city(), r.country());
        return PartnerMapper.toSupplier(suppliers.save(s));
    }

    @Override
    public SupplierResponse update(UUID id, UpdateSupplierRequest r) {
        Supplier s = find(id);
        apply(s, r.name(), r.contactPerson(), r.phone(), r.email(), r.addressLine(), r.city(), r.country());
        return PartnerMapper.toSupplier(s);
    }

    @Override
    public SupplierResponse activate(UUID id) {
        Supplier s = find(id);
        s.setActive(true);
        return PartnerMapper.toSupplier(s);
    }

    @Override
    public SupplierResponse deactivate(UUID id) {
        Supplier s = find(id);
        guards.check(DeactivationGuard.Target.SUPPLIER, id);       // e.g. inbound: open purchase orders
        s.setActive(false);
        return PartnerMapper.toSupplier(s);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<SupplierResponse> search(String q, Pageable pageable) {
        return suppliers.search(Codes.likePattern(q), pageable)
                .map(PartnerMapper::toSupplier);
    }

    @Override @Transactional(readOnly = true)
    public SupplierResponse getById(UUID id) {
        return PartnerMapper.toSupplier(find(id));
    }

    private Supplier find(UUID id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier", id));
    }

    private void apply(Supplier s, String name, String contact, String phone, String email,
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

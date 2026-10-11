package rw.afriteck.wms.foundation.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import rw.afriteck.wms.foundation.dtos.*;

import java.util.UUID;

public interface CustomerService {
    
    CustomerResponse create(CreateCustomerRequest r);

    CustomerResponse update(UUID id, UpdateCustomerRequest r);

    CustomerResponse activate(UUID id);

    CustomerResponse deactivate(UUID id);

    Page<CustomerResponse> search(String q, Pageable pageable);

    CustomerResponse getById(UUID id);            // lookup for outbound
}

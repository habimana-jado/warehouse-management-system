package rw.afriteck.wms.foundation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.foundation.entity.Customer;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
    boolean existsByCodeIgnoreCase(String code);

    @Query("""
           select s from Customer s
           where lower(s.code) like :pattern
              or lower(s.name) like :pattern
              or lower(s.phone) like :pattern
           """)
    Page<Customer> search(@Param("pattern") String pattern, Pageable pageable);
}

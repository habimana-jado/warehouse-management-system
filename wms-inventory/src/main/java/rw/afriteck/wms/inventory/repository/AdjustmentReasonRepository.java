package rw.afriteck.wms.inventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inventory.entity.AdjustmentReason;

import java.util.Optional;
import java.util.UUID;

public interface AdjustmentReasonRepository extends JpaRepository<AdjustmentReason, UUID> {
    Optional<AdjustmentReason> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
            select r from AdjustmentReason r
            where (r.active = true or :includeInactive = true)
              and (lower(r.code) like :pattern or lower(r.name) like :pattern)""")
    Page<AdjustmentReason> search(@Param("pattern") String pattern,
                                  @Param("includeInactive") boolean includeInactive, Pageable pageable);
}

package rw.afriteck.wms.inbound.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inbound.entity.CustomerReturn;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CustomerReturnRepository extends JpaRepository<CustomerReturn, UUID> {

    @Query(value = "select nextval('return_number_seq')", nativeQuery = true)
    long nextReturnNumber();

    @EntityGraph(attributePaths = "lines")
    Optional<CustomerReturn> findWithLinesById(UUID id);

    /** Locks the return before receive or cancel; lines are read lazily afterwards. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CustomerReturn r where r.id = :id")
    Optional<CustomerReturn> findByIdForUpdate(@Param("id") UUID id);

    /**
     * `statuses` must be NON-EMPTY (pass all values when the user did not filter) and `pattern` non-null
     * (pass "%" for no search text).
     */
    @Query("""
            select r from CustomerReturn r
             where r.warehouseId = :warehouseId
               and r.status in :statuses
               and (lower(r.returnNumber) like :pattern
                    or lower(coalesce(r.salesOrderNumber, '')) like :pattern)
            """)
    Page<CustomerReturn> search(@Param("warehouseId") UUID warehouseId,
                                @Param("statuses") Collection<CustomerReturnStatus> statuses,
                                @Param("pattern") String pattern,
                                Pageable pageable);

    /** ItemInboundGuard: pass Set.of(CustomerReturnStatus.EXPECTED). */
    boolean existsByLines_ItemIdAndStatusIn(UUID itemId, Collection<CustomerReturnStatus> statuses);
}
package rw.afriteck.wms.outbound.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.outbound.entity.SupplierReturn;
import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SupplierReturn r where r.id = :id")
    Optional<SupplierReturn> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByLines_ItemIdAndStatusIn(UUID itemId, Collection<SupplierReturnStatus> statuses);   // ItemOutboundGuard

    @Query(value = "select r from SupplierReturn r where r.warehouseId = :wh and r.status in :statuses "
            + "and (lower(r.returnNumber) like :pattern or lower(coalesce(r.supplierReference, '')) like :pattern)",
            countQuery = "select count(r) from SupplierReturn r where r.warehouseId = :wh and r.status in :statuses "
                    + "and (lower(r.returnNumber) like :pattern or lower(coalesce(r.supplierReference, '')) like :pattern)")
    Page<SupplierReturn> search(@Param("wh") UUID warehouseId, @Param("statuses") Collection<SupplierReturnStatus> statuses,
                                @Param("pattern") String pattern, Pageable pageable);

    @Query("select l.supplierReturn.id as parentId, count(l) as total from SupplierReturnLine l "
            + "where l.supplierReturn.id in :ids group by l.supplierReturn.id")
    List<ParentCount> countLines(@Param("ids") Collection<UUID> ids);
}

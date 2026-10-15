package rw.afriteck.wms.inbound.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inbound.entity.PurchaseOrder;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>, JpaSpecificationExecutor<PurchaseOrder> {

    /** Sequence value for PO numbers (formatted by InboundNumbers). */
    @Query(value = "select nextval('po_number_seq')", nativeQuery = true)
    long nextPoNumber();

    /** Detail screens: header plus lines in one query (never use with paging). */
    @EntityGraph(attributePaths = "lines")
    Optional<PurchaseOrder> findWithLinesById(UUID id);

    /**
     * Locks the header row. Every service method that changes a PO's lines or status starts here.
     * Lines are then read lazily inside the same transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PurchaseOrder po where po.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") UUID id);

    // ---- deactivation guards: always pass PurchaseOrderStatus.OPEN (never an empty collection)

    boolean existsBySupplierIdAndStatusIn(UUID supplierId, Collection<PurchaseOrderStatus> statuses);

    /** Underscore makes the path explicit: lines -> itemId. */
    boolean existsByLines_ItemIdAndStatusIn(UUID itemId, Collection<PurchaseOrderStatus> statuses);
}
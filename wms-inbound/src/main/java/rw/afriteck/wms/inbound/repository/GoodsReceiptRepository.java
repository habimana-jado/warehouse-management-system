package rw.afriteck.wms.inbound.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inbound.entity.GoodsReceipt;
import rw.afriteck.wms.inbound.enums.PutawayStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    @Query(value = "select nextval('receipt_number_seq')", nativeQuery = true)
    long nextReceiptNumber();

    @EntityGraph(attributePaths = "lines")
    Optional<GoodsReceipt> findWithLinesById(UUID id);

    /** Receipts of one PO, newest first (PO detail screen). */
    List<GoodsReceipt> findByPurchaseOrderIdOrderByReceivedAtDesc(UUID purchaseOrderId);

    /** `pattern` must be non-null: pass "%" when the user typed nothing. */
    @Query("""
            select r from GoodsReceipt r
             where r.warehouseId = :warehouseId
               and (lower(r.receiptNumber) like :pattern
                    or lower(coalesce(r.deliveryNoteNumber, '')) like :pattern)
            """)
    Page<GoodsReceipt> search(@Param("warehouseId") UUID warehouseId,
                              @Param("pattern") String pattern,
                              Pageable pageable);

    /** Receipts that still have at least one line in the given put-away status (the "waiting for put-away" board). */
    @Query(value = """
            select r from GoodsReceipt r
             where r.warehouseId = :warehouseId
               and exists (select 1 from GoodsReceiptLine l
                            where l.goodsReceipt = r and l.putawayStatus = :status)
            """,
            countQuery = """
            select count(r) from GoodsReceipt r
             where r.warehouseId = :warehouseId
               and exists (select 1 from GoodsReceiptLine l
                            where l.goodsReceipt = r and l.putawayStatus = :status)
            """)
    Page<GoodsReceipt> findWithLinesInPutawayStatus(@Param("warehouseId") UUID warehouseId,
                                                    @Param("status") PutawayStatus status,
                                                    Pageable pageable);
}
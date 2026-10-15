package rw.afriteck.wms.inbound.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.afriteck.wms.inbound.entity.GoodsReceiptLine;
import rw.afriteck.wms.inbound.enums.PutawayStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, UUID> {

    /**
     * Put-away confirmation locks the line first, so two workers cannot confirm it twice.
     * The receipt is then read lazily in the same transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from GoodsReceiptLine l where l.id = :id")
    Optional<GoodsReceiptLine> findByIdForUpdate(@Param("id") UUID id);

    interface ReceiptCount {
        UUID getReceiptId();
        Long getLineCount();
    }

    @Query("""
        select l.goodsReceipt.id as receiptId, count(l) as lineCount
          from GoodsReceiptLine l
         where l.goodsReceipt.id in :receiptIds
         group by l.goodsReceipt.id
        """)
    List<ReceiptCount> countByReceipt(@Param("receiptIds") Collection<UUID> receiptIds);

    /** Pass PutawayStatus.PENDING as a parameter, never as a literal. */
    @Query("""
        select l.goodsReceipt.id as receiptId, count(l) as lineCount
          from GoodsReceiptLine l
         where l.goodsReceipt.id in :receiptIds and l.putawayStatus = :status
         group by l.goodsReceipt.id
        """)
    List<ReceiptCount> countByReceiptAndPutawayStatus(@Param("receiptIds") Collection<UUID> receiptIds,
                                                      @Param("status") PutawayStatus status);
}
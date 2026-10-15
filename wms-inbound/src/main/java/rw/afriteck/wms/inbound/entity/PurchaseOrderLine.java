package rw.afriteck.wms.inbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderLine extends BaseEntity {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    /** Item BASE UOM. */
    @Column(name = "ordered_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal orderedQty;

    /** Accepted so far (this is what fulfils the line). */
    @Column(name = "accepted_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal acceptedQty = BigDecimal.ZERO;

    /** Arrived but refused at inspection (posted to a DAMAGED bin). Does not fulfil the line. */
    @Column(name = "rejected_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal rejectedQty = BigDecimal.ZERO;

    /** Per BASE unit. Null = unknown; receipts then post a null unit cost. */
    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /** Extra quantity the receiver may accept above orderedQty, in percent. 0 = exact. */
    @Column(name = "tolerance_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal tolerancePercent = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    /** Still needed to fulfil the order (never negative). */
    public BigDecimal remainingQty() {
        return orderedQty.subtract(acceptedQty).max(BigDecimal.ZERO);
    }

    /** Highest total accepted quantity allowed: ordered + tolerance. */
    public BigDecimal maxAcceptableQty() {
        return orderedQty
                .multiply(HUNDRED.add(tolerancePercent))
                .divide(HUNDRED, 4, RoundingMode.DOWN);
    }

    /** How much more may still be accepted on this line, tolerance included. */
    public BigDecimal acceptableQtyLeft() {
        return maxAcceptableQty().subtract(acceptedQty).max(BigDecimal.ZERO);
    }

    public boolean isFulfilled() {
        return acceptedQty.compareTo(orderedQty) >= 0;
    }
}

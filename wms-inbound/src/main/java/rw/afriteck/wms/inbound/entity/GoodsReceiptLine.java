package rw.afriteck.wms.inbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inbound.enums.PutawayStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_lines")
@Getter
@Setter
@NoArgsConstructor
public class GoodsReceiptLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_line_id", nullable = false)
    private PurchaseOrderLine purchaseOrderLine;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    /** Copied from the PO line (avoids loading it for list screens). */
    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    /** Normalized with Codes.normalize, exactly as inventory and operations store it. */
    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    /** Item BASE UOM. */
    @Column(name = "accepted_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal acceptedQty = BigDecimal.ZERO;

    @Column(name = "rejected_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal rejectedQty = BigDecimal.ZERO;

    /** Mandatory when rejectedQty > 0. */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** Where the accepted stock was posted: a RECEIVING bin (PUTAWAY) or a STORAGE bin (DIRECT). */
    @Column(name = "receiving_location_id")
    private UUID receivingLocationId;

    /** DAMAGED bin that received the rejected quantity. */
    @Column(name = "reject_location_id")
    private UUID rejectLocationId;

    /** PO price at receipt time (stored so later PO edits never change history). Null = unknown. */
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "putaway_status", nullable = false, length = 20)
    private PutawayStatus putawayStatus = PutawayStatus.NOT_REQUIRED;

    @Column(name = "suggested_location_id")
    private UUID suggestedLocationId;

    /** The bin the worker actually used. */
    @Column(name = "putaway_location_id")
    private UUID putawayLocationId;

    @Column(name = "putaway_done_qty", precision = 19, scale = 4)
    private BigDecimal putawayDoneQty;

    /** Line in the operations task (plain uuid). */
    @Column(name = "task_line_id")
    private UUID taskLineId;
}

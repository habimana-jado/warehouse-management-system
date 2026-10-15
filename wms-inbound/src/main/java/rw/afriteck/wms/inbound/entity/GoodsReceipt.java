package rw.afriteck.wms.inbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inbound.enums.ReceiptMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
public class GoodsReceipt extends BaseEntity {

    @Column(name = "receipt_number", nullable = false, length = 30, updatable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /** Copied from the PO so receipts can be filtered and access-checked without loading it. */
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceiptMode mode;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "received_by", nullable = false)
    private UUID receivedBy;

    /** The supplier's delivery note number. */
    @Column(name = "delivery_note_number", length = 100)
    private String deliveryNoteNumber;

    @Column(length = 1000)
    private String notes;

    /** PUTAWAY task in wms-operations; null for DIRECT receipts. */
    @Column(name = "task_id")
    private UUID taskId;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    public void addLine(GoodsReceiptLine line) {
        lines.add(line);
        line.setGoodsReceipt(this);
    }
}
package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inventory.enums.AdjustmentStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_adjustments",
        uniqueConstraints = @UniqueConstraint(name = "uq_adjustment_number", columnNames = "adjustment_number"),
        indexes = @Index(name = "ix_adjustment_wh_status", columnList = "warehouse_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class StockAdjustment extends BaseEntity {

    @Column(name = "adjustment_number", nullable = false, length = 20)
    private String adjustmentNumber;                    // ADJ-000123

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reason_id", nullable = false)
    private AdjustmentReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdjustmentStatus status = AdjustmentStatus.PENDING_APPROVAL;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;                           // approve() rejects decidedBy == requestedBy

    @Column(name = "decided_by")                        // approver or rejecter
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<StockAdjustmentLine> lines = new ArrayList<>();
}

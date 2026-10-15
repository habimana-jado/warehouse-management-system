package rw.afriteck.wms.inbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inbound.enums.PurchaseOrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder extends BaseEntity {

    @Column(name = "po_number", nullable = false, length = 30, updatable = false)
    private String poNumber;

    /** Plain ids: supplier and warehouse live in wms-foundation. */
    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    /** The supplier's own order or quote number. */
    @Column(name = "supplier_reference", length = 100)
    private String supplierReference;

    @Column(length = 1000)
    private String notes;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    /** ISO 4217 code. Defaults to RWF; any other 3-letter code is allowed. */
    @Column(nullable = false, length = 3)
    private String currency = "RWF";

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }
}

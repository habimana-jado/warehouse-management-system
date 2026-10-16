package rw.afriteck.wms.outbound.entity;

import rw.afriteck.wms.outbound.enums.SupplierReturnStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "supplier_returns")
@Getter
@Setter
@NoArgsConstructor
public class SupplierReturn extends BaseEntity {

    @Column(name = "return_number", nullable = false, length = 20)
    private String returnNumber;                      // RTV-000123

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierReturnStatus status = SupplierReturnStatus.DRAFT;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false, length = 500)
    private String reason;                            // mandatory

    @Column(name = "supplier_reference", length = 60)
    private String supplierReference;                 // free text (credit note, PO number...): outbound does not know inbound

    @Column(length = 1000)
    private String notes;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "posted_at")
    private Instant postedAt;

    @OneToMany(mappedBy = "supplierReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<SupplierReturnLine> lines = new ArrayList<>();

    public void addLine(SupplierReturnLine line) {
        line.setSupplierReturn(this);
        lines.add(line);
    }
}


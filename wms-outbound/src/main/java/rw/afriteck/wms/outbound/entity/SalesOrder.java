package rw.afriteck.wms.outbound.entity;

import rw.afriteck.wms.outbound.enums.SalesOrderStatus;
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
@Table(name = "sales_orders")
@Getter
@Setter
@NoArgsConstructor
public class SalesOrder extends BaseEntity {

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;                       // SO-000123

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SalesOrderStatus status = SalesOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Column(nullable = false, length = 3)
    private String currency;                          // default RWF, set by the service

    @Column(name = "customer_reference", length = 60)
    private String customerReference;

    @Column(length = 1000)
    private String notes;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<SalesOrderLine> lines = new ArrayList<>();

    public void addLine(SalesOrderLine line) {
        line.setSalesOrder(this);
        lines.add(line);
    }

    /** True when anything was picked; cancel is only allowed while this is false. */
    public boolean hasPickedStock() {
        return lines.stream().anyMatch(l -> l.getPickedQty().signum() > 0);
    }
}
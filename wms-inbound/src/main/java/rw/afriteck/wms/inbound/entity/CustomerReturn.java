package rw.afriteck.wms.inbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inbound.enums.CustomerReturnStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_returns")
@Getter
@Setter
@NoArgsConstructor
public class CustomerReturn extends BaseEntity {

    @Column(name = "return_number", nullable = false, length = 30, updatable = false)
    private String returnNumber;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    /** Optional (walk-in returns). Plain id into wms-foundation. */
    @Column(name = "customer_id")
    private UUID customerId;

    /** The original sales order, by plain reference only (outbound is a different module). */
    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "sales_order_number", length = 50)
    private String salesOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerReturnStatus status = CustomerReturnStatus.EXPECTED;

    @Column(length = 1000)
    private String notes;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "customerReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<CustomerReturnLine> lines = new ArrayList<>();

    public void addLine(CustomerReturnLine line) {
        lines.add(line);
        line.setCustomerReturn(this);
    }
}

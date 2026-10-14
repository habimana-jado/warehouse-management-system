package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inventory.enums.CountLineStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_count_lines",
        uniqueConstraints = @UniqueConstraint(name = "uq_count_line_no", columnNames = {"count_id", "line_no"}))
@Getter
@Setter
@NoArgsConstructor
public class StockCountLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "count_id", nullable = false)
    private StockCount count;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(name = "serial_number", length = 64)
    private String serialNumber;

    @Column(name = "expected_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQty;                     // snapshot when the count starts

    @Column(name = "counted_qty", precision = 19, scale = 4)
    private BigDecimal countedQty;                      // null until counted

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CountLineStatus status = CountLineStatus.PENDING;

    @Column(name = "counted_by")
    private UUID countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(name = "task_id")
    private UUID taskId;                 // the COUNT task that carries this line (null for lines added during the count)

    @Column(name = "task_line_id")
    private UUID taskLineId;
}

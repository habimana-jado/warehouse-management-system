package rw.afriteck.wms.operations.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.operations.enums.TaskLineStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "task_line", uniqueConstraints =
@UniqueConstraint(name = "uq_task_line_no", columnNames = {"task_id", "line_no"}))
@Getter
@Setter
@NoArgsConstructor
public class TaskLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;
    @Column(name = "lot_number", length = 60)
    private String lotNumber;                  // when item is LOT-tracked
    @Column(name = "serial_number", length = 60)
    private String serialNumber;               // when SERIAL-tracked

    @Column(name = "from_location_id") private UUID fromLocationId;
    @Column(name = "to_location_id")   private UUID toLocationId;

    @Column(name = "planned_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedQty;             // always in item base UOM
    @Column(name = "done_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal doneQty = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TaskLineStatus status = TaskLineStatus.PENDING;

    @Column(name = "reference_line_id") private UUID referenceLineId; // PO line, SO line, ...
    @Column(name = "short_reason", length = 255) private String shortReason;
}

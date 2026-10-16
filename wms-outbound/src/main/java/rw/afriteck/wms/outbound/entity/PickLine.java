package rw.afriteck.wms.outbound.entity;

import rw.afriteck.wms.outbound.enums.PickLineStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pick_lines")
@Getter @Setter @NoArgsConstructor
public class PickLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pick_list_id", nullable = false)
    private PickList pickList;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_line_id", nullable = false)
    private SalesOrderLine salesOrderLine;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "planned_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedQty;

    @Column(name = "picked_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal pickedQty = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PickLineStatus status = PickLineStatus.PENDING;

    /** Suggestions only (most stock first; earliest-expiring lot for expiry-tracked items). The picker may choose otherwise. */
    @Column(name = "suggested_location_id")
    private UUID suggestedLocationId;

    @Column(name = "suggested_lot_number", length = 60)
    private String suggestedLotNumber;

    @Column(name = "task_line_id")
    private UUID taskLineId;

    @Column(name = "short_reason", length = 500)
    private String shortReason;

    @OneToMany(mappedBy = "pickLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PickAllocation> allocations = new ArrayList<>();

    public void addAllocation(PickAllocation a) {
        a.setPickLine(this);
        allocations.add(a);
    }
}

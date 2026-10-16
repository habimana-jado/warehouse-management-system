package rw.afriteck.wms.outbound.entity;

import rw.afriteck.wms.outbound.enums.PickListStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pick_lists")
@Getter
@Setter
@NoArgsConstructor
public class PickList extends BaseEntity {

    @Column(name = "pick_number", nullable = false, length = 20)
    private String pickNumber;                        // PCK-000123

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PickListStatus status = PickListStatus.RELEASED;

    @Column(name = "task_id")
    private UUID taskId;                              // the PICK task in wms-operations

    @Column(name = "released_at", nullable = false)
    private Instant releasedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @OneToMany(mappedBy = "pickList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<PickLine> lines = new ArrayList<>();

    public void addLine(PickLine line) {
        line.setPickList(this);
        lines.add(line);
    }
}


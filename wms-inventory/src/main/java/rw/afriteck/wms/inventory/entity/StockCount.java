package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inventory.enums.CountStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_counts",
        uniqueConstraints = @UniqueConstraint(name = "uq_count_number", columnNames = "count_number"),
        indexes = @Index(name = "ix_count_wh_status", columnList = "warehouse_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class StockCount extends BaseEntity {

    @Column(name = "count_number", nullable = false, length = 20)         // CNT-000123
    private String countNumber;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "zone_location_id")                  // optional scope: a ZONE of the warehouse
    private UUID zoneLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CountStatus status = CountStatus.DRAFT;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "count", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<StockCountLine> lines = new ArrayList<>();
}

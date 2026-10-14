package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inventory.enums.TransferStatus;
import rw.afriteck.wms.inventory.enums.TransferType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_transfers",
        uniqueConstraints = @UniqueConstraint(name = "uq_transfer_number", columnNames = "transfer_number"),
        indexes = {
                @Index(name = "ix_transfer_source", columnList = "source_warehouse_id, status"),
                @Index(name = "ix_transfer_dest", columnList = "destination_warehouse_id, status")
        })
@Getter
@Setter
@NoArgsConstructor
public class StockTransfer extends BaseEntity {

    @Column(name = "transfer_number", nullable = false, length = 20)      // TRF-000123
    private String transferNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransferType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "source_warehouse_id", nullable = false)
    private UUID sourceWarehouseId;

    @Column(name = "destination_warehouse_id", nullable = false)          // equals source for BIN transfers
    private UUID destinationWarehouseId;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<StockTransferLine> lines = new ArrayList<>();
}

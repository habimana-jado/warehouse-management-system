package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inventory.enums.MovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "stock_movements",
        indexes = {
                @Index(name = "ix_movement_wh_item_time", columnList = "warehouse_id, item_id, occurred_at"),
                @Index(name = "ix_movement_location", columnList = "location_id"),
                @Index(name = "ix_movement_reference", columnList = "reference_type, reference_id"),
                @Index(name = "ix_movement_transaction", columnList = "transaction_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class StockMovement extends BaseEntity {

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;                        // groups both sides of a transfer

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24, updatable = false)
    private MovementType type;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private UUID warehouseId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", updatable = false)
    private Lot lot;

    @Column(name = "serial_number", length = 64, updatable = false)
    private String serialNumber;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal quantity;                       // SIGNED delta, never zero

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal balanceAfter;                   // balance of this exact key after the move

    @Column(name = "unit_cost", precision = 19, scale = 4, updatable = false)
    private BigDecimal unitCost;

    @Column(name = "reference_type", length = 32, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "reference_number", length = 64, updatable = false)
    private String referenceNumber;

    @Column(name = "note", length = 500, updatable = false)
    private String note;
}

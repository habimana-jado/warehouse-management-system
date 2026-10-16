package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stock_adjustment_lines",
        uniqueConstraints = @UniqueConstraint(name = "uq_adjustment_line_no", columnNames = {"adjustment_id", "line_no"}))
@Getter
@Setter
@NoArgsConstructor
public class StockAdjustmentLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adjustment_id", nullable = false)
    private StockAdjustment adjustment;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "lot_number", length = 64)           // normalized; the Lot is resolved when posting
    private String lotNumber;

    @Column(name = "expiry_date")                       // only used if the lot has to be created
    private LocalDate expiryDate;

    @Column(name = "serial_number", length = 64)
    private String serialNumber;

    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDelta;                   // signed, non-zero, base UOM
}

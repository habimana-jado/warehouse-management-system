package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer_lines",
        uniqueConstraints = @UniqueConstraint(name = "uq_transfer_line_no", columnNames = {"transfer_id", "line_no"}))
@Getter
@Setter
@NoArgsConstructor
public class StockTransferLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(name = "serial_number", length = 64)
    private String serialNumber;

    @Column(name = "from_location_id", nullable = false)
    private UUID fromLocationId;

    @Column(name = "to_location_id")                    // null until receiving for INTER_WAREHOUSE
    private UUID toLocationId;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;                        // planned, base UOM

    @Column(name = "shipped_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippedQty = BigDecimal.ZERO;

    @Column(name = "received_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQty = BigDecimal.ZERO;   // in transit = shippedQty - receivedQty

    @Column(name = "short_reason", length = 500)
    private String shortReason;        // set when a line is closed short (planned BIN confirm, or close-short on receive)
}

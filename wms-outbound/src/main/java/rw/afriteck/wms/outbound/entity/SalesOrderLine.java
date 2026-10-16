package rw.afriteck.wms.outbound.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sales_order_lines")
@Getter
@Setter
@NoArgsConstructor
public class SalesOrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    /** All quantities and the price are in the item BASE UOM (box entries are converted at entry). */
    @Column(name = "ordered_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal orderedQty;

    /** Quantity on pick lines that are still open, plus quantity already picked. Reduced when a pick line closes short. */
    @Column(name = "released_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal releasedQty = BigDecimal.ZERO;

    @Column(name = "picked_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal pickedQty = BigDecimal.ZERO;

    @Column(name = "shipped_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippedQty = BigDecimal.ZERO;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;                     // null = unknown, never summed as 0

    @Column(length = 255)
    private String note;

    /** Still to be put on a pick list (backorders included). */
    public BigDecimal unreleasedQty() { return orderedQty.subtract(releasedQty).max(BigDecimal.ZERO); }

    /** Picked but not yet on a shipment. */
    public BigDecimal readyToShipQty() { return pickedQty.subtract(shippedQty).max(BigDecimal.ZERO); }

    public boolean isFulfilled() { return shippedQty.compareTo(orderedQty) >= 0; }
}


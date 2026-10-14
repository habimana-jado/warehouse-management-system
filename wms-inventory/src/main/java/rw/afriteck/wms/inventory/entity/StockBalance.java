package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_balances",
        indexes = {
                @Index(name = "ix_balance_wh_item", columnList = "warehouse_id, item_id"),
                @Index(name = "ix_balance_location", columnList = "location_id"),
                @Index(name = "ix_balance_item_lot", columnList = "item_id, lot_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class StockBalance extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;                                   // null unless trackingType = LOT

    @Column(name = "serial_number", length = 64)       // null unless trackingType = SERIAL
    private String serialNumber;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;     // base UOM; serial rows hold 0 or 1
}

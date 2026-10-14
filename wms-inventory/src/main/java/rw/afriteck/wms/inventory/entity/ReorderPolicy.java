package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reorder_policies",
        uniqueConstraints = @UniqueConstraint(name = "uq_reorder_wh_item", columnNames = {"warehouse_id", "item_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ReorderPolicy extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "min_qty", nullable = false, precision = 19, scale = 4)       // reorder point
    private BigDecimal minQty;

    @Column(name = "reorder_qty", nullable = false, precision = 19, scale = 4)   // suggested order size
    private BigDecimal reorderQty;

    @Column(name = "max_qty", precision = 19, scale = 4)
    private BigDecimal maxQty;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

package rw.afriteck.wms.foundation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;

@Entity
@Table(name = "item_uoms",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "uom"}))
@Getter
@Setter
public class ItemUom extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String uom;                  // BOX, PALLET

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal factorToBase;     // 1 BOX = 12 EA -> 12

    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

}

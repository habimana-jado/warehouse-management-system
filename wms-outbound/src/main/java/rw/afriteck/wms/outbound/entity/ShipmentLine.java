package rw.afriteck.wms.outbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shipment_lines")
@Getter @Setter @NoArgsConstructor
public class ShipmentLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_line_id", nullable = false)
    private SalesOrderLine salesOrderLine;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;                      // base UOM, > 0
}

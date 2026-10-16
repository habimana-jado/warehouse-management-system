package rw.afriteck.wms.outbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "supplier_return_lines")
@Getter @Setter @NoArgsConstructor
public class SupplierReturnLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_return_id", nullable = false)
    private SupplierReturn supplierReturn;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;                          // an active BIN, STORAGE or DAMAGED

    @Column(name = "lot_number", length = 60)
    private String lotNumber;

    @Column(name = "serial_number", length = 60)
    private String serialNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;                      // positive, base UOM; the service posts it as a negative ISSUE

    @Column(length = 255)
    private String note;
}

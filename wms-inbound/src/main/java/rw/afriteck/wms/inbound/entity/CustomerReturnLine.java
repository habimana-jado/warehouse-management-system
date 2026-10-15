package rw.afriteck.wms.inbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inbound.enums.ReturnDisposition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customer_return_lines")
@Getter
@Setter
@NoArgsConstructor
public class CustomerReturnLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_return_id", nullable = false)
    private CustomerReturn customerReturn;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    /** Item BASE UOM. */
    @Column(name = "expected_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQty;

    /** Filled when the return is received (may differ from expected, may be zero). */
    @Column(name = "received_qty", precision = 19, scale = 4)
    private BigDecimal receivedQty;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    /** Why the customer returned it. */
    @Column(name = "return_reason", length = 500)
    private String returnReason;

    /** Set at receiving, after inspection. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReturnDisposition disposition;

    /** Bin that received the stock (STORAGE for RESTOCK, DAMAGED for DAMAGED). */
    @Column(name = "location_id")
    private UUID locationId;
}

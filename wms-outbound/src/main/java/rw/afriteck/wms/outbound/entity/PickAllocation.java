package rw.afriteck.wms.outbound.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID; /** What the picker actually took: one row per bin and lot, or per serial unit. Each row is one ISSUE in the ledger. */
@Entity
@Table(name = "pick_allocations")
@Getter @Setter @NoArgsConstructor
public class PickAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pick_line_id", nullable = false)
    private PickLine pickLine;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;                          // a STORAGE bin

    @Column(name = "lot_number", length = 60)
    private String lotNumber;                         // normalized, LOT items only

    @Column(name = "serial_number", length = 60)
    private String serialNumber;                      // normalized, SERIAL items only (quantity 1)

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;                      // positive, base UOM

    @Column(name = "expiry_override", nullable = false)
    private boolean expiryOverride;                   // a manager allowed an expired lot
}

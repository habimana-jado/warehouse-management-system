package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.inventory.enums.AdjustmentDirection;

@Entity
@Table(name = "adjustment_reasons",
        uniqueConstraints = @UniqueConstraint(name = "uq_adjustment_reason_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class AdjustmentReason extends BaseEntity {

    @Column(name = "code", nullable = false, length = 32)
    private String code;                               // Codes.normalize, e.g. DAMAGED, EXPIRED, FOUND

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private AdjustmentDirection direction = AdjustmentDirection.BOTH;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

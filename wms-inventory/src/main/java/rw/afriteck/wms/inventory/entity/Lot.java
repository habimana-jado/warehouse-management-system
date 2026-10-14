package rw.afriteck.wms.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lots",
        uniqueConstraints = @UniqueConstraint(name = "uq_lot_item_number", columnNames = {"item_id", "lot_number"}),
        indexes = @Index(name = "ix_lot_expiry", columnList = "expiry_date"))
@Getter
@Setter
@NoArgsConstructor
public class Lot extends BaseEntity {

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "lot_number", nullable = false, length = 64)
    private String lotNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;                                // only for items with expiryTracked
}

package rw.afriteck.wms.foundation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.foundation.enums.LocationType;
import rw.afriteck.wms.foundation.enums.LocationUsage;

import java.math.BigDecimal;

@Entity
@Table(name = "locations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "code"}))
@Getter
@Setter
public class Location extends BaseEntity {

    @Column(nullable = false, length = 40)
    private String code;                 // e.g. A-03-02-B

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationUsage usage = LocationUsage.STORAGE;

    @Column(unique = true)
    private String barcode;              // printed on the bin label

    private BigDecimal maxCapacity;      // optional, in base units or volume

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Location parent;

}

package rw.afriteck.wms.foundation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;
import rw.afriteck.wms.foundation.enums.TrackingType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Getter
@Setter
public class Item extends BaseEntity {

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    private String description;

    @Column(unique = true)
    private String barcode;

    @Column(nullable = false, length = 10)
    private String baseUom;              // EA, KG, ...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingType trackingType = TrackingType.NONE;

    @Column(nullable = false)
    private boolean expiryTracked = false;   // only meaningful when trackingType = LOT

    private BigDecimal unitCost;         // optional, for valuation reports
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemUom> uoms = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ItemCategory category;

}

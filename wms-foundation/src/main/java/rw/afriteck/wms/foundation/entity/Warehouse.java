package rw.afriteck.wms.foundation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
public class Warehouse extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    private String addressLine;
    private String city;
    private String country;

    @Column(nullable = false)
    private boolean active = true;
}

package rw.afriteck.wms.foundation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rw.afriteck.wms.common.persistence.BaseEntity;

@Entity
@Table(name = "item_categories")
@Getter
@Setter
public class ItemCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
